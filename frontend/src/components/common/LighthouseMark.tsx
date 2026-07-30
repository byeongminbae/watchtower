import Image from "next/image";

export default function LighthouseMark({ size = 28 }: { size?: number }) {
  return (
    <Image
      src="/watchtower.png"
      alt=""
      width={size}
      height={size}
      sizes={`${size}px`}
      style={{ display: "block", width: size, height: size }}
    />
  );
}
