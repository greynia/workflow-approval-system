import { policyReferenceKey } from "./ai-review-summary";

describe("policyReferenceKey", () => {
  it("includes section because chunkIndex is only stable within a section", () => {
    const first = policyReferenceKey({
      section: "新進員工請假限制",
      source: "leave-policy.zh.md",
      chunkIndex: 0,
      content: "新進員工規定",
      score: 0.9,
    });
    const second = policyReferenceKey({
      section: "事假規定",
      source: "leave-policy.zh.md",
      chunkIndex: 0,
      content: "事假規定",
      score: 0.8,
    });

    expect(first).toBe("leave-policy.zh.md-新進員工請假限制-0");
    expect(second).toBe("leave-policy.zh.md-事假規定-0");
    expect(first).not.toBe(second);
  });
});
