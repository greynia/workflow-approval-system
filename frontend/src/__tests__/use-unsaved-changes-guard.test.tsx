import { act, renderHook } from "@testing-library/react";
import { useUnsavedChangesGuard } from "@/lib/use-unsaved-changes-guard";
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store";

beforeEach(() => {
  useUnsavedChangesStore.setState({ dirty: false, pending: null });
});

describe("useUnsavedChangesGuard", () => {
  it("syncs the store with the `when` flag and clears on unmount", () => {
    const { rerender, unmount } = renderHook(
      ({ when }: { when: boolean }) => useUnsavedChangesGuard({ when }),
      { initialProps: { when: false } }
    );
    expect(useUnsavedChangesStore.getState().dirty).toBe(false);

    rerender({ when: true });
    expect(useUnsavedChangesStore.getState().dirty).toBe(true);

    rerender({ when: false });
    expect(useUnsavedChangesStore.getState().dirty).toBe(false);

    rerender({ when: true });
    unmount();
    expect(useUnsavedChangesStore.getState().dirty).toBe(false);
  });

  it("registers a beforeunload listener only while `when` is true", () => {
    const addSpy = jest.spyOn(window, "addEventListener");
    const removeSpy = jest.spyOn(window, "removeEventListener");

    const { rerender, unmount } = renderHook(
      ({ when }: { when: boolean }) => useUnsavedChangesGuard({ when }),
      { initialProps: { when: false } }
    );

    expect(addSpy).not.toHaveBeenCalledWith("beforeunload", expect.any(Function));

    rerender({ when: true });
    expect(addSpy).toHaveBeenCalledWith("beforeunload", expect.any(Function));

    rerender({ when: false });
    expect(removeSpy).toHaveBeenCalledWith("beforeunload", expect.any(Function));

    rerender({ when: true });
    unmount();
    expect(removeSpy).toHaveBeenCalledWith("beforeunload", expect.any(Function));

    addSpy.mockRestore();
    removeSpy.mockRestore();
  });

  it("preventDefault is called on the beforeunload event so the browser shows its native prompt", () => {
    let captured: ((event: BeforeUnloadEvent) => void) | undefined;
    const addSpy = jest
      .spyOn(window, "addEventListener")
      .mockImplementation((type, handler) => {
        if (type === "beforeunload") {
          captured = handler as (event: BeforeUnloadEvent) => void;
        }
      });

    renderHook(() => useUnsavedChangesGuard({ when: true }));
    expect(captured).toBeDefined();

    const event = { preventDefault: jest.fn() } as unknown as BeforeUnloadEvent;
    act(() => {
      captured?.(event);
    });
    expect(event.preventDefault).toHaveBeenCalled();

    addSpy.mockRestore();
  });
});
