import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import React from "react";
import { afterEach, vi } from "vitest";

vi.mock("next/image", () => ({
  default: ({
    src,
    alt,
    width,
    height,
    sizes,
    style,
    className,
  }: {
    readonly src: string;
    readonly alt: string;
    readonly width: number;
    readonly height: number;
    readonly sizes?: string;
    readonly style?: React.CSSProperties;
    readonly className?: string;
  }) =>
    React.createElement("img", {
      src,
      alt,
      width,
      height,
      sizes,
      style,
      className,
    }),
}));

afterEach(cleanup);
