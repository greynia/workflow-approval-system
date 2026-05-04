import { act, renderHook } from "@testing-library/react";
import { useGuardedNavigation } from "@/lib/use-guarded-navigation";
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store";

const replace = jest.fn();
const push = jest.fn();

jest.mock("@/i18n/navigation", () => ({
  useRouter: () => ({ replace, push }),
}));

beforeEach(() => {
  useUnsavedChangesStore.setState({ dirty: false, pending: null });
  replace.mockClear();
  push.mockClear();
});

function makeMouseEvent(overrides: Partial<MouseEvent> = {}): React.MouseEvent {
  const event = {
    metaKey: false,
    ctrlKey: false,
    shiftKey: false,
    altKey: false,
    button: 0,
    defaultPrevented: false,
    preventDefault: jest.fn(function (this: { defaultPrevented: boolean }) {
      this.defaultPrevented = true;
    }),
    ...overrides,
  };
  return event as unknown as React.MouseEvent;
}

describe("useGuardedNavigation", () => {
  it("invokes the underlying router immediately when the form is clean", () => {
    const { result } = renderHook(() => useGuardedNavigation());
    act(() => result.current.replace({ pathname: "/x" }, { locale: "en" }));
    expect(replace).toHaveBeenCalledTimes(1);
    expect(useUnsavedChangesStore.getState().pending).toBeNull();
  });

  it("queues a pending action instead of navigating when the form is dirty", () => {
    useUnsavedChangesStore.setState({ dirty: true });
    const { result } = renderHook(() => useGuardedNavigation());

    act(() => result.current.replace({ pathname: "/x" }, { locale: "en" }));
    expect(replace).not.toHaveBeenCalled();

    const pending = useUnsavedChangesStore.getState().pending;
    expect(pending).toBeInstanceOf(Function);

    act(() => useUnsavedChangesStore.getState().confirm());
    expect(replace).toHaveBeenCalledTimes(1);
    expect(useUnsavedChangesStore.getState().dirty).toBe(false);
    expect(useUnsavedChangesStore.getState().pending).toBeNull();
  });

  it("runGuarded executes arbitrary actions through the same prompt", () => {
    useUnsavedChangesStore.setState({ dirty: true });
    const action = jest.fn();
    const { result } = renderHook(() => useGuardedNavigation());

    act(() => result.current.runGuarded(action));
    expect(action).not.toHaveBeenCalled();

    act(() => useUnsavedChangesStore.getState().confirm());
    expect(action).toHaveBeenCalledTimes(1);
  });

  it("interceptLinkClick lets modifier-key clicks fall through (new tab)", () => {
    useUnsavedChangesStore.setState({ dirty: true });
    const { result } = renderHook(() => useGuardedNavigation());
    const event = makeMouseEvent({ metaKey: true });

    act(() => result.current.interceptLinkClick("/balances", event));
    expect(event.preventDefault).not.toHaveBeenCalled();
    expect(useUnsavedChangesStore.getState().pending).toBeNull();
  });

  it("interceptLinkClick blocks plain left-clicks while dirty and queues navigation", () => {
    useUnsavedChangesStore.setState({ dirty: true });
    const { result } = renderHook(() => useGuardedNavigation());
    const event = makeMouseEvent();

    act(() => result.current.interceptLinkClick("/balances", event));
    expect(event.preventDefault).toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();

    act(() => useUnsavedChangesStore.getState().confirm());
    expect(push).toHaveBeenCalledWith("/balances");
  });

  it("interceptLinkClick is a no-op when the form is clean", () => {
    const { result } = renderHook(() => useGuardedNavigation());
    const event = makeMouseEvent();

    act(() => result.current.interceptLinkClick("/balances", event));
    expect(event.preventDefault).not.toHaveBeenCalled();
    expect(useUnsavedChangesStore.getState().pending).toBeNull();
  });

  it("cancel discards the pending action without invoking it", () => {
    useUnsavedChangesStore.setState({ dirty: true });
    const { result } = renderHook(() => useGuardedNavigation());

    act(() => result.current.push("/x"));
    expect(useUnsavedChangesStore.getState().pending).toBeInstanceOf(Function);

    act(() => useUnsavedChangesStore.getState().cancel());
    expect(push).not.toHaveBeenCalled();
    expect(useUnsavedChangesStore.getState().pending).toBeNull();
    expect(useUnsavedChangesStore.getState().dirty).toBe(true);
  });
});
