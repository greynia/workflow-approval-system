import type { Meta, StoryObj } from "@storybook/nextjs-vite"
import { useState } from "react"
import { BarChart3, CheckSquare, FileText, Settings } from "lucide-react"

import { AppSidebar, type SidebarItem } from "./app-sidebar"
import zhTW from "@/i18n/messages/zh-TW.json"
import en from "@/i18n/messages/en.json"

type Messages = typeof en

const msgMap: Record<string, Messages> = { "zh-TW": zhTW, en }

function getSidebarItems(msgs: Messages): SidebarItem[] {
  return [
    { id: "requests", label: msgs.Sidebar.MyRequests, href: "/requests", icon: FileText, badge: 2 },
    { id: "approvals", label: msgs.Sidebar.PendingApprovals, href: "/approvals", icon: CheckSquare, badge: 5, matchPaths: ["/approvals"] },
    { id: "balances", label: msgs.Sidebar.LeaveBalances, href: "/balances", icon: BarChart3 },
    { id: "admin", label: msgs.Sidebar.AuditLog, href: "/admin/audit-logs", icon: Settings },
  ]
}

function SidebarWrapper({
  locale,
  initialOpen = false,
  initialCollapsed = false,
  pathname,
  enableCollapse = false,
}: {
  locale: string
  initialOpen?: boolean
  initialCollapsed?: boolean
  pathname?: string
  enableCollapse?: boolean
}) {
  const [open, setOpen] = useState(initialOpen)
  const [collapsed, setCollapsed] = useState(initialCollapsed)
  const msgs = msgMap[locale] ?? zhTW
  return (
    <div className="flex h-screen">
      <AppSidebar
        items={getSidebarItems(msgs)}
        open={open}
        onOpenChange={setOpen}
        pathname={pathname}
        collapsed={collapsed}
        onCollapsedChange={enableCollapse ? setCollapsed : undefined}
        brand={
          <span className="truncate text-sm font-semibold">
            {collapsed ? "W" : "Workflow"}
          </span>
        }
      />
      <div className="flex-1 bg-muted/40 p-6">
        <p className="text-sm text-muted-foreground">Main content area</p>
      </div>
    </div>
  )
}

const meta: Meta<typeof AppSidebar> = {
  title: "Layout/AppSidebar",
  component: AppSidebar,
  tags: ["autodocs"],
  parameters: { layout: "fullscreen" },
}
export default meta

type Story = StoryObj<typeof AppSidebar>

export const Desktop: Story = {
  render: (_, { globals }) => (
    <SidebarWrapper locale={(globals.locale as string) ?? "zh-TW"} />
  ),
}

export const WithActiveItem: Story = {
  name: "Desktop (active: Balances)",
  render: (_, { globals }) => (
    <SidebarWrapper
      locale={(globals.locale as string) ?? "zh-TW"}
      pathname="/balances"
    />
  ),
}

export const MobileOpen: Story = {
  render: (_, { globals }) => (
    <SidebarWrapper locale={(globals.locale as string) ?? "zh-TW"} initialOpen />
  ),
  globals: { viewport: { value: "mobile1", isRotated: false } },
}

export const Collapsed: Story = {
  name: "Desktop (collapsed)",
  render: (_, { globals }) => (
    <SidebarWrapper
      locale={(globals.locale as string) ?? "zh-TW"}
      initialCollapsed
      enableCollapse
    />
  ),
}

export const Collapsible: Story = {
  name: "Desktop (toggleable)",
  render: (_, { globals }) => (
    <SidebarWrapper
      locale={(globals.locale as string) ?? "zh-TW"}
      enableCollapse
    />
  ),
}

export const Loading: Story = {
  render: () => {
    const [open, setOpen] = useState(false)
    return (
      <div className="flex h-screen">
        <AppSidebar
          items={[]}
          open={open}
          onOpenChange={setOpen}
          loading
          brand={<span className="text-sm font-semibold">Workflow</span>}
        />
        <div className="flex-1 bg-muted/40 p-6" />
      </div>
    )
  },
}
