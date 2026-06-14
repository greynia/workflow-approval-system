import { useState } from "react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";

import {
  AuditLogFilterBar,
  type AuditLogFilterLabels,
  type AuditLogFilterValues,
} from "./audit-log-filter-bar";

const LABELS: AuditLogFilterLabels = {
  entityType: "Entity Type",
  action: "Action",
  actorName: "Actor Name",
  createdFrom: "From (Operation Time)",
  createdTo: "To (Operation Time)",
  entityTypePlaceholder: "All types",
  actionPlaceholder: "All actions",
  actorNamePlaceholder: "Search by name...",
};

const ENTITY_TYPE_OPTIONS = [
  { value: "LEAVE_REQUEST", label: "Leave Request" },
];

const ACTION_OPTIONS = [
  { value: "CREATE", label: "CREATE" },
  { value: "APPROVE", label: "APPROVE" },
  { value: "REJECT", label: "REJECT" },
  { value: "CANCEL", label: "CANCEL" },
];

const meta: Meta<typeof AuditLogFilterBar> = {
  title: "Audit/AuditLogFilterBar",
  component: AuditLogFilterBar,
};
export default meta;

type Story = StoryObj<typeof AuditLogFilterBar>;

export const Empty: Story = {
  render: () => {
    const [value, setValue] = useState<AuditLogFilterValues>({});
    return (
      <AuditLogFilterBar
        value={value}
        onChange={setValue}
        entityTypeOptions={ENTITY_TYPE_OPTIONS}
        actionOptions={ACTION_OPTIONS}
        labels={LABELS}
      />
    );
  },
};

export const WithValues: Story = {
  render: () => {
    const [value, setValue] = useState<AuditLogFilterValues>({
      entityType: "LEAVE_REQUEST",
      action: "APPROVE",
      actorName: "Alice",
      createdFrom: "2026-04-01T00:00",
      createdTo: "2026-04-30T23:59",
    });
    return (
      <AuditLogFilterBar
        value={value}
        onChange={setValue}
        entityTypeOptions={ENTITY_TYPE_OPTIONS}
        actionOptions={ACTION_OPTIONS}
        labels={LABELS}
      />
    );
  },
};
