import { useAuthority } from "@/hooks/useAuthority";

describe("useAuthority", () => {
  it("returns true when authority list is empty (open-by-default)", () => {
    expect(useAuthority([], [])).toBe(true);
    expect(useAuthority(["request.view"], [])).toBe(true);
  });

  it("returns false when userAuthority is empty but authority is required", () => {
    expect(useAuthority([], ["audit.view"])).toBe(false);
  });

  it("returns true when userAuthority contains a required permission", () => {
    expect(useAuthority(["audit.view", "request.view"], ["audit.view"])).toBe(true);
  });

  it("returns false when userAuthority has no matching permission", () => {
    expect(useAuthority(["request.view"], ["audit.view"])).toBe(false);
  });

  it("returns true on any-match (OR semantics) when multiple authorities required", () => {
    expect(useAuthority(["request.view"], ["audit.view", "request.view"])).toBe(true);
  });

  it("uses default empty arrays — returns true (open-by-default)", () => {
    expect(useAuthority()).toBe(true);
  });
});
