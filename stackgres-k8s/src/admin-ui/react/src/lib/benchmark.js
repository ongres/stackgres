// pgbench benchmark data transformations (SGDbOps detail charts).

// transactionsPerSecond.overTime.values is delta-encoded: each value is the
// difference from the previous one; decode with a running prefix sum.
export function decodeDeltaValues(values) {
  const decoded = [...(values || [])]
  for (let i = 1; i < decoded.length; i++) {
    decoded[i] += decoded[i - 1]
  }
  return decoded
}

export function statementsCategories(statements) {
  return (statements || []).map((statement) => statement.command)
}

export function statementsLatencies(statements) {
  return (statements || []).map((statement) => statement.latency)
}
