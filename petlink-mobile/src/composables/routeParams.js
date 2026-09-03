export function currentPageParams(){
  const pages = getCurrentPages()
  return pages[pages.length-1]?.options || {}
}
