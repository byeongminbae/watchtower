import Image from "next/image";

export default function LighthouseMark({ size = 28 }: { size?: number }) {
  return <Image src="/watchtower-icon.svg" width={size} height={size} alt="" className="shrink-0" aria-hidden="true" />;
}
