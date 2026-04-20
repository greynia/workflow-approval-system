import { screen, fireEvent } from "@testing-library/react";
import { ErrorBoundary } from "@/components/ui/error-boundary";
import { renderWithProviders } from "./test-utils";

// Suppress React's built-in error boundary console output in tests
beforeEach(() => {
  jest.spyOn(console, "error").mockImplementation(() => {});
});
afterEach(() => {
  (console.error as jest.Mock).mockRestore();
});

let shouldThrow = true;
function ControlledThrow() {
  if (shouldThrow) throw new Error("boom");
  return <p>recovered</p>;
}

beforeEach(() => {
  shouldThrow = true;
});

describe("ErrorBoundary", () => {
  it("renders children when no error occurs", () => {
    renderWithProviders(
      <ErrorBoundary>
        <p>hello</p>
      </ErrorBoundary>
    );
    expect(screen.getByText("hello")).toBeInTheDocument();
  });

  it("shows default fallback on throw and recovers after retry click", () => {
    renderWithProviders(
      <ErrorBoundary>
        <ControlledThrow />
      </ErrorBoundary>
    );
    expect(screen.getByRole("button", { name: "重試" })).toBeInTheDocument();
    shouldThrow = false;
    fireEvent.click(screen.getByRole("button", { name: "重試" }));
    expect(screen.getByText("recovered")).toBeInTheDocument();
  });

  it("shows custom fallback when fallback prop is provided", () => {
    renderWithProviders(
      <ErrorBoundary fallback={<p>custom error</p>}>
        <ControlledThrow />
      </ErrorBoundary>
    );
    expect(screen.getByText("custom error")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "重試" })).not.toBeInTheDocument();
  });
});
