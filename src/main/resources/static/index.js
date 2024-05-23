const socket = new WebSocket("ws://localhost:8080/ws/chat");

let socketUserInfo = {
    messageType : ''
}
function testSend() {
    let data = {
        messageType : socketUserInfo.messageType,
        chatRoomId : 1,
        senderId : 1,
        message : document.querySelector('#socket-input-area').value,
    }
    socket.send(JSON.stringify(data));
    socketUserInfo.messageType = 'TALK'
    document.querySelector('#socket-input-area').value = ''; // 전송 후 초기화
}

socket.onopen = function() {
    socketUserInfo.messageType = 'ENTER'
    console.log('소켓 열림');
};

// 소켓을 통해 메시지를 받을 경우
socket.onmessage = function(event) {
    const eventResult = JSON.parse(event.data);
    if (!eventResult.message) {
        return;
    }
    const msg = document.createElement('li');
    msg.textContent = eventResult.message;
    document.querySelector('#msg-area').appendChild(msg);
};

socket.onclose = function(event) {
    console.log('소켓 닫음');
};


// 방송 이벤트 관련
const eventSource = new EventSource('http://localhost:8080/streamer/start');

eventSource.onmessage = function(event) {
    const notifications = document.getElementById('notify-area');
    const notification = document.createElement('li');
    notification.textContent = event.data;
    notifications.appendChild(notification);
};

eventSource.onerror = function(event) {
    console.error("SSE connection error:", event);
};