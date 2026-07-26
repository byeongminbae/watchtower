export default function LighthouseMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* 밤바다 원형 배경 */}
      <circle cx="20" cy="20" r="20" fill="#11202F" />
      {/* 노란 신호 불빛 */}
      <circle cx="20" cy="11" r="6" fill="#FFC94A" fillOpacity="0.35" />
      {/* 등대 탑 */}
      <path d="M17 14H23L25 32H15L17 14Z" fill="#EAF2F8" />
      <rect x="16.2" y="17" width="7.6" height="2.2" fill="#1F4E79" />
      <rect x="15.4" y="22" width="9.2" height="2.2" fill="#1F4E79" />
      {/* 등대 램프실 */}
      <rect x="16.5" y="9.5" width="7" height="5" rx="1" fill="#FFC94A" />
      <path d="M18 6L20 3L22 6H18Z" fill="#EAF2F8" />
      {/* 파도 */}
      <path
        d="M6 33C8 31.5 10 31.5 12 33C14 34.5 16 34.5 18 33C20 31.5 22 31.5 24 33C26 34.5 28 34.5 30 33C32 31.5 34 31.5 34 33"
        stroke="#3B82C4"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </svg>
  );
}
