import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

describe("Vitest environment", () => {
  it("Given a rendered element, when queried by role, then exposes jest-dom matchers", () => {
    render(<button type="button">Ready</button>);

    expect(screen.getByRole("button", { name: "Ready" })).toBeInTheDocument();
  });
});
