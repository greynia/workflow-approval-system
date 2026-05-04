import type { Meta, StoryObj } from "@storybook/nextjs-vite";

import { Progress } from "./progress";

const meta: Meta<typeof Progress> = {
  title: "UI/Progress",
  component: Progress,
  tags: ["autodocs"],
  parameters: {
    layout: "centered",
  },
};
export default meta;

type Story = StoryObj<typeof Progress>;

export const Default: Story = {
  render: () => (
    <div className="w-80">
      <Progress value={35} />
    </div>
  ),
};

export const Empty: Story = {
  render: () => (
    <div className="w-80">
      <Progress value={0} />
    </div>
  ),
};

export const Full: Story = {
  render: () => (
    <div className="w-80">
      <Progress value={100} />
    </div>
  ),
};

export const Tones: Story = {
  name: "Usage Tone Variants",
  render: () => (
    <div className="flex w-80 flex-col gap-5">
      <UsageRow label="Healthy (35%)" value={35} indicatorClassName="bg-primary" />
      <UsageRow label="Caution (65%)" value={65} indicatorClassName="bg-warning" />
      <UsageRow label="Danger (90%)" value={90} indicatorClassName="bg-destructive" />
    </div>
  ),
};

export const BalanceCardExample: Story = {
  name: "In Balance Card Context",
  render: () => (
    <div className="w-80 rounded-lg border border-border bg-card p-5">
      <p className="text-sm font-medium text-foreground">Annual Leave</p>
      <p className="mt-1">
        <span className="text-2xl font-semibold text-foreground">9d</span>
        <span className="text-muted-foreground"> / 14d</span>
      </p>
      <div className="mt-4 flex flex-col gap-1.5">
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span>Usage</span>
          <span className="font-medium text-foreground">36%</span>
        </div>
        <Progress value={36} indicatorClassName="bg-primary" />
      </div>
    </div>
  ),
};

function UsageRow({
  label,
  value,
  indicatorClassName,
}: {
  label: string;
  value: number;
  indicatorClassName: string;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">{label}</span>
        <span className="font-medium text-foreground">{value}%</span>
      </div>
      <Progress value={value} indicatorClassName={indicatorClassName} />
    </div>
  );
}
