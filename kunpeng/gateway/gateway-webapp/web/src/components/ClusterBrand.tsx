/** 品牌标识：内联 SVG 标记（无外部图片资产），KUNPENG 控制台 */
export default function ClusterBrand({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`cluster-brand ${compact ? 'cluster-brand--compact' : ''}`}>
      <span className="cluster-brand__mark" aria-hidden="true">
        <svg viewBox="0 0 39 39" role="presentation">
          <defs>
            <linearGradient id="kb-mark" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stopColor="#4d8dff" />
              <stop offset="1" stopColor="#1f54d6" />
            </linearGradient>
          </defs>
          <rect width="39" height="39" rx="12" fill="url(#kb-mark)" />
          <path
            d="M13 27V12l13 15V12"
            fill="none"
            stroke="#ffffff"
            strokeWidth="3.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
      {!compact && (
        <span className="cluster-brand__text">
          <strong>KUNPENG</strong>
          <small>微服务编排引擎</small>
        </span>
      )}
    </div>
  );
}
