import type { Meta, StoryObj } from "@storybook/nextjs-vite"

import { DescriptionItem, DescriptionList } from "./description-list"

const meta: Meta<typeof DescriptionList> = {
  title: "UI/DescriptionList",
  component: DescriptionList,
  tags: ["autodocs"],
}
export default meta

type Story = StoryObj<typeof DescriptionList>

export const TwoColumns: Story = {
  render: () => (
    <DescriptionList columns={2}>
      <DescriptionItem term="Applicant">Wang Xiaoming</DescriptionItem>
      <DescriptionItem term="Leave Type">Annual Leave</DescriptionItem>
      <DescriptionItem term="Start Date">2026-03-10</DescriptionItem>
      <DescriptionItem term="End Date">2026-03-12</DescriptionItem>
      <DescriptionItem term="Duration">3 days</DescriptionItem>
      <DescriptionItem term="Status">Pending</DescriptionItem>
    </DescriptionList>
  ),
}

export const OneColumn: Story = {
  render: () => (
    <DescriptionList columns={1}>
      <DescriptionItem term="Reason">Planned family vacation during the spring festival period.</DescriptionItem>
      <DescriptionItem term="Notes">No shift coverage required.</DescriptionItem>
    </DescriptionList>
  ),
}
