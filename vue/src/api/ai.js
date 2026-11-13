import request from '@/utils/request.js';

const getToken = () => {
    const userStr = localStorage.getItem('code_user');
    if (!userStr) return '';
    try {
        return JSON.parse(userStr).token || '';
    } catch (e) {
        return '';
    }
};

const baseUrl = '/api';

export function previewSplitTask(data) {
    return request.post('/ai/task/split/preview', data);
}

export function streamSplitTask(projectId, { requirement, studentCount, deadline, onProgress, onResult, onError, onDone, abortSignal }) {
    const params = { projectId, requirement, studentCount, deadline };
    return streamGet('/ai/task/split/stream', params,
        (data) => {
            if (data.startsWith('[PROGRESS]')) {
                if (onProgress) onProgress(data.substring(10));
            } else if (data.startsWith('[RESULT]')) {
                if (onResult) {
                    try {
                        onResult(JSON.parse(data.substring(8)));
                    } catch (e) {
                        if (onError) onError('结果解析失败');
                    }
                }
            }
        },
        onError,
        onDone,
        abortSignal
    );
}

export function confirmSplitTask(data) {
    return request.post('/ai/task/split/confirm', data);
}

export function streamRiskAnalyze(projectId, { onMessage, onError, onDone, abortSignal }) {
    return streamGet('/ai/risk/analyze', { projectId }, onMessage, onError, onDone, abortSignal);
}

export function listAiLogs(params) {
  return request.get('/ai/logs', { params });
}

export function getAiLogStats() {
  return request.get('/ai/logs/stats');
}

export function interruptAi() {
  return request.post('/ai/interrupt');
}

function streamGet(url, params, onMessage, onError, onDone, abortSignal) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
        if (v != null) query.append(k, v);
    });
    const fullUrl = `${baseUrl}${url}?${query.toString()}`;
    const token = getToken();
    const controller = new AbortController();
    if (abortSignal) {
        abortSignal.addEventListener('abort', () => controller.abort());
    }

    fetch(fullUrl, {
        method: 'GET',
        headers: { 'token': token },
        signal: controller.signal
    }).then(async response => {
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';
        let done = false;
        while (!done) {
            const { value, done: d } = await reader.read();
            done = d;
            if (value) {
                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n\n');
                buffer = lines.pop();
                lines.forEach(line => parseSseLine(line, onMessage, onError));
            }
        }
        if (buffer.trim()) parseSseLine(buffer, onMessage, onError);
        if (onDone) onDone();
    }).catch(err => {
        if (err.name === 'AbortError') {
            if (onDone) onDone();
            return;
        }
        if (onError) onError(err.message || 'SSE 连接失败');
        if (onDone) onDone();
    });
}

function parseSseLine(raw, onMessage, onError) {
    raw.split('\n').forEach(line => {
        if (line.startsWith('data:')) {
            const data = line.substring(5).trim();
            if (!data) return;
            if (data.startsWith('[ERROR]')) {
                if (onError) onError(data.substring(7));
                return;
            }
            if (onMessage) onMessage(data);
        }
    });
}
