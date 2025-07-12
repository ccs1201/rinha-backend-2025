import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

// Custom metrics for each interval
const summary5s = new Trend('summary_5s_duration');
const summary10s = new Trend('summary_10s_duration');
const summary15s = new Trend('summary_15s_duration');
const summary20s = new Trend('summary_20s_duration');

export const options = {
  stages: [
    { duration: '30s', target: 100 }, // Populate data with high concurrency
    { duration: '60s', target: 2 }, // Test summary endpoints - max 2 VUs
  ],
  thresholds: {
    'summary_5s_duration': ['p(98)<400', 'p(99)<500'],
    'summary_10s_duration': ['p(98)<800', 'p(99)<1000'],
    'summary_15s_duration': ['p(98)<1200', 'p(99)<1500'],
    'summary_20s_duration': ['p(98)<1600', 'p(99)<2000'],
  },
  summaryTrendStats: ['avg', 'p(98)', 'p(99)', 'count'],
  discardResponseBodies: true,
};

const BASE_URL = 'http://localhost:9999';
let baseTime = new Date();

export function setup() {
  // Purge existing data
  http.post(`${BASE_URL}/purge-payments`);
  console.log('Data purged, starting population...');
  
  baseTime = new Date();
  return { baseTime: baseTime.toISOString() };
}

export default function(data) {
  const currentTime = new Date();
  const stage = getCurrentStage();
  
  if (stage === 'populate') {
    populatePayments();
  } else if (stage === 'test') {
    testSummaryEndpoints();
  }
}

function getCurrentStage() {
  const elapsed = (new Date() - new Date(baseTime)) / 1000;
  return elapsed < 30 ? 'populate' : 'test';
}

function populatePayments() {
  const now = new Date();
  const payload = {
    correlationId: generateUUID(),
    amount: Math.round((Math.random() * 1000 + 10) * 100) / 100,
  };
  
  const response = http.post(`${BASE_URL}/payments`, JSON.stringify(payload), {
    headers: { 'Content-Type': 'application/json' },
    tags: { name: 'create_payment' },
  });
  
  check(response, {
    'payment created': (r) => r.status === 200,
  });
}

function testSummaryEndpoints() {
  const now = new Date();
  
  // Test different time intervals
  testInterval(5, now, summary5s);
  testInterval(10, now, summary10s);
  testInterval(15, now, summary15s);
  testInterval(20, now, summary20s);
}

function testInterval(seconds, now, metric) {
  const to = now.toISOString();
  const from = new Date(now.getTime() - (seconds * 1000)).toISOString();
  
  const response = http.get(`${BASE_URL}/payments-summary?from=${from}&to=${to}`, {
    tags: { name: 'get_summary', interval: `${seconds}s` }
  });
  
  check(response, {
    [`summary ${seconds}s success`]: (r) => r.status === 200,
    [`summary ${seconds}s has data`]: (r) => {
      try {
        const data = JSON.parse(r.body);
        return data.default !== undefined && data.fallback !== undefined;
      } catch {
        return false;
      }
    },
  });
  
  if (response.status === 200) {
    metric.add(response.timings.duration);
  }
}

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

export function teardown(data) {
  console.log('\n=== PERFORMANCE SUMMARY ===');
  console.log('Population: 100 VUs for 30s (~50k+ payments)');
  console.log('Testing: 2 VUs for 60s (summary queries)');
  console.log('Key metrics (avg, p98, p99, count):');
  console.log('- summary_5s_duration: 5-second interval queries');
  console.log('- summary_10s_duration: 10-second interval queries');
  console.log('- summary_15s_duration: 15-second interval queries');
  console.log('- summary_20s_duration: 20-second interval queries');
}