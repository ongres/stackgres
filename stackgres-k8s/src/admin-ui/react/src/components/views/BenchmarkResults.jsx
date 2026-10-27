import { useEffect, useRef } from 'react'
import Chart from 'react-apexcharts'
import HdrHistogramWidget from 'hdr-histogram-widget'
import { decodeDeltaValues, statementsCategories, statementsLatencies } from '../../lib/benchmark'

function HdrHistogram({ percentiles }) {
  const containerRef = useRef(null)

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.innerHTML = ''
      HdrHistogramWidget.display({ percentiles }, 'milliseconds', containerRef.current)
    }
  }, [percentiles])

  return <div ref={containerRef} />
}

// pgbench results of a benchmark SGDbOps, ported from the Vue SGDbOps view:
// TPS-over-time line, time-percentiles HDR histogram, per-statement latencies.
export default function BenchmarkResults({ dbop }) {
  const pgbench = dbop.status?.benchmark?.pgbench
  if (dbop.spec.op !== 'benchmark' || !pgbench) {
    return null
  }

  return (
    <section className="flex flex-col gap-6">
      {pgbench.transactionsPerSecond?.overTime && (
        <div>
          <h3 className="mb-2 font-semibold uppercase">TPS over-time</h3>
          <Chart
            type="line"
            height={350}
            options={{
              chart: { id: 'tpsOverTimeGraph', animations: { enabled: false } },
              stroke: { width: 1 },
              yaxis: { title: { text: 'TPS' } },
              xaxis: {
                labels: { show: false },
                axisBorder: { show: false },
                axisTicks: { show: false },
              },
              tooltip: { shared: false },
            }}
            series={[
              {
                name: 'TPS',
                data: decodeDeltaValues(pgbench.transactionsPerSecond.overTime.values),
              },
            ]}
          />
        </div>
      )}
      {pgbench.hdrHistogram && (
        <div>
          <h3 className="mb-2 font-semibold uppercase">Time Percentiles</h3>
          <HdrHistogram percentiles={pgbench.hdrHistogram} />
        </div>
      )}
      {pgbench.statements && (
        <div>
          <h3 className="mb-2 font-semibold uppercase">Statements</h3>
          <Chart
            type="bar"
            height={350}
            options={{
              chart: { id: 'statementsGraph', animations: { enabled: false } },
              plotOptions: { bar: { horizontal: true } },
              dataLabels: { enabled: false },
              xaxis: { categories: statementsCategories(pgbench.statements) },
            }}
            series={[{ data: statementsLatencies(pgbench.statements) }]}
          />
        </div>
      )}
    </section>
  )
}
