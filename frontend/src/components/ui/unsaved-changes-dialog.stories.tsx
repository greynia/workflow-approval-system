import { useEffect } from "react"
import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { Button } from "./button"
import { UnsavedChangesDialog } from "./unsaved-changes-dialog"
import { useUnsavedChangesStore } from "@/stores/unsaved-changes-store"

const meta: Meta = {
  title: "UI/UnsavedChangesDialog",
  tags: ["autodocs"],
  parameters: { layout: "centered" },
}
export default meta
type Story = StoryObj

function StoryHarness({ openOnMount = false }: { openOnMount?: boolean }) {
  useEffect(() => {
    if (openOnMount) {
      useUnsavedChangesStore.setState({
        dirty: true,
        pending: () => alert("Navigation confirmed"),
      })
    }
    return () => useUnsavedChangesStore.setState({ dirty: false, pending: null })
  }, [openOnMount])

  return (
    <div className="flex flex-col items-center gap-4">
      <Button
        onClick={() =>
          useUnsavedChangesStore.setState({
            dirty: true,
            pending: () => alert("Navigation confirmed"),
          })
        }
      >
        Trigger guard
      </Button>
      <UnsavedChangesDialog />
    </div>
  )
}

export const Closed: Story = {
  name: "Closed (idle)",
  render: () => <StoryHarness />,
}

export const Open: Story = {
  name: "Open (pending action)",
  render: () => <StoryHarness openOnMount />,
}
