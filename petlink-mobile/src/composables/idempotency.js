export function idempotencyHeaders(key){return {'Idempotency-Key':String(key||'')}}
export function normalizeFollowupPayload({content,healthCondition,imageTokens=[],idempotencyKey}){
  return {content:String(content||'').trim()||null,healthCondition:String(healthCondition||'').trim()||null,imageTokens:[...imageTokens],idempotencyKey:String(idempotencyKey||'').toLowerCase()}
}
