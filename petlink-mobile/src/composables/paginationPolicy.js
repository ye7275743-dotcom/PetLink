export function mergePageRecords(current=[], incoming=[], key='id'){
  const seen=new Set(current.map(item=>item?.[key]))
  return current.concat((incoming||[]).filter(item=>!seen.has(item?.[key])))
}
export function hasMorePages({page=1,pages=0,total=0,count=0}={}){
  return pages>0 ? page<pages : count<total
}
