export function validTrendRange(range){if(!Array.isArray(range)||range.length!==2)return false;const days=(Date.parse(range[1])-Date.parse(range[0]))/86400000+1;return Number.isFinite(days)&&days>=1&&days<=366}
export function chartPercent(value,peak){return Math.max(0,Math.min(100,Number(value)/Math.max(1,Number(peak))*90))}
