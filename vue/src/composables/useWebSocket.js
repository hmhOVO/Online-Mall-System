import {ref, onUnmounted} from 'vue';

let wsInstance = null;
let listeners = [];

export function useWebSocket() {
    const isConnected = ref(false);

    const connect = () => {
        if (wsInstance && wsInstance.readyState === WebSocket.OPEN) {
            isConnected.value = true;
            return;
        }

        const userStr = localStorage.getItem('code_user');
        if (!userStr) return;
        
        const user = JSON.parse(userStr);
        if (!user.token) return;

        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/notification?token=${user.token}`;

        wsInstance = new WebSocket(wsUrl);

        wsInstance.onopen = () => {
            isConnected.value = true;
            console.log('WebSocket 连接成功');
        };

        wsInstance.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                listeners.forEach(listener => {
                    try {
                        listener(data);
                    } catch (e) {
                        console.error('WebSocket 消息处理失败', e);
                    }
                });
            } catch (e) {
                console.error('WebSocket 消息解析失败', e);
            }
        };

        wsInstance.onerror = (error) => {
            console.error('WebSocket 连接异常', error);
        };

        wsInstance.onclose = (event) => {
            isConnected.value = false;
            if (event.code === 1008) {
                const currentUser = JSON.parse(localStorage.getItem('code_user') || '{}');
                if (currentUser.token) {
                    localStorage.removeItem('code_user');
                    window.location.href = '/login';
                }
                return;
            }
            if (event.code === 1000) {
                return;
            }
            setTimeout(() => {
                const currentUser = JSON.parse(localStorage.getItem('code_user') || '{}');
                if (currentUser.token) {
                    connect();
                }
            }, 5000);
        };
    };

    const addListener = (callback) => {
        listeners.push(callback);
        return () => {
            listeners = listeners.filter(l => l !== callback);
        };
    };

    const removeListener = (callback) => {
        listeners = listeners.filter(l => l !== callback);
    };

    const disconnect = () => {
        if (wsInstance) {
            wsInstance.close();
            wsInstance = null;
        }
        isConnected.value = false;
    };

    const init = () => {
        connect();
    };

    return {
        isConnected,
        connect,
        disconnect,
        init,
        addListener,
        removeListener
    };
}
