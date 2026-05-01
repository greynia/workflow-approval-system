import type { Meta, StoryObj } from "@storybook/nextjs-vite"
import { FileX } from "lucide-react"

import { Button } from "./button"
import { DataTable, type ColumnDef } from "./data-table"
import { EmptyState } from "./empty-state"
import { StatusBadge } from "./status-badge"

type Row = {
  id: number
  applicant: string
  type: string
  startDate: string
  endDate: string
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED"
}

const SAMPLE: Row[] = [
  { id: 1, applicant: "Wang Xiaoming", type: "Annual", startDate: "2026-03-10", endDate: "2026-03-12", status: "PENDING" },
  { id: 2, applicant: "Li Meimei", type: "Sick", startDate: "2026-02-20", endDate: "2026-02-21", status: "APPROVED" },
  { id: 3, applicant: "Zhang Wei", type: "Personal", startDate: "2026-01-05", endDate: "2026-01-05", status: "REJECTED" },
]

const COLUMNS: ColumnDef<Row>[] = [
  { id: "applicant", header: "Applicant", cell: (r) => r.applicant },
  { id: "type", header: "Type", cell: (r) => r.type, hideOnMobile: true },
  { id: "period", header: "Period", cell: (r) => `${r.startDate} – ${r.endDate}`, hideOnMobile: true },
  { id: "status", header: "Status", cell: (r) => <StatusBadge status={r.status} label={r.status} /> },
]

const meta: Meta<typeof DataTable> = {
  title: "UI/DataTable",
  component: DataTable,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof DataTable>

export const Default: Story = {
  render: () => (
    <DataTable
      columns={COLUMNS}
      data={SAMPLE}
      keyField={(r) => r.id}
      getRowHref={(r) => `/requests/${r.id}`}
    />
  ),
}

export const Loading: Story = {
  render: () => (
    <DataTable
      columns={COLUMNS}
      data={[]}
      keyField={(r) => r.id}
      loading
      loadingRows={4}
    />
  ),
}

export const Empty: Story = {
  render: () => (
    <DataTable
      columns={COLUMNS}
      data={[]}
      keyField={(r) => r.id}
      emptyState={
        <EmptyState
          icon={<FileX />}
          title="No requests found"
          description="You haven't submitted any leave requests yet."
          action={<Button size="sm">New Request</Button>}
        />
      }
    />
  ),
}

export const WithRowActions: Story = {
  render: () => (
    <DataTable
      columns={COLUMNS}
      data={SAMPLE.filter((r) => r.status === "PENDING")}
      keyField={(r) => r.id}
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      rowActions={(_r) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="success">Approve</Button>
          <Button size="sm" variant="destructive">Reject</Button>
        </div>
      )}
    />
  ),
}

export const MobileCard: Story = {
  render: () => (
    <DataTable
      columns={COLUMNS}
      data={SAMPLE}
      keyField={(r) => r.id}
      renderMobileCard={(r) => (
        <div className="flex items-center justify-between rounded-lg border border-border bg-card p-3">
          <div>
            <p className="text-sm font-medium">{r.applicant}</p>
            <p className="text-xs text-muted-foreground">{r.type} · {r.startDate}</p>
          </div>
          <StatusBadge status={r.status} label={r.status} />
        </div>
      )}
    />
  ),
}
