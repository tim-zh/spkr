navigator.getUserMedia = ( navigator.getUserMedia ||
navigator.webkitGetUserMedia ||
navigator.mozGetUserMedia);

function getSessionCookie(key) {
  var value = Cookies.get("ssession");
  if (! value)
    return undefined;
  value = value.substring(value.indexOf("-") + 1);
  return value.split("&").map(function(x) {
    return x.split("=");
  }).filter(function(x) {
    return x[0] == key;
  })[0][1];
}

function createAudio(arraybuffer) {
  return $("<audio/>", {
    controls: true,
    autoplay: true,
    src: URL.createObjectURL(new Blob([arraybuffer], { type: "audio/ogg" }))
  });
}

(function() {
  var recorder;
  var recordData;
  var isRecording = false;
  var replayAudioTag;
  var soundLevelIntervalId;
  var soundLevelSpanTag;

  function startRecording(stream) {
    stopRecording();
    recorder = new MediaRecorder(stream);
    recorder.mimeType = 'audio/ogg';
    recorder.ondataavailable = function(e) {
      replayAudioTag.src = URL.createObjectURL(e.data);
      replayAudioTag.style.display = "inline";
      recordData = e.data;
    };
    replayAudioTag.style.display = "none";

    recorder.start();
    isRecording = true;

    soundLevelIntervalId = visualize(soundLevelSpanTag, stream);
  }

  function stopRecording() {
    if (! recorder)
      return;
    isRecording = false;
    if (recorder.state != "inactive") {
      recorder.stop();
      recorder.stream.stop();
    }

    clearInterval(soundLevelIntervalId);
    soundLevelIntervalId = undefined;
    soundLevelSpanTag.style.paddingRight = 0;
  }

  function visualize(soundLevelSpanTag, stream) {
    var audioCtx = new (window.AudioContext || webkitAudioContext)();
    var source = audioCtx.createMediaStreamSource(stream);
    var analyser = audioCtx.createAnalyser();
    analyser.fftSize = 32;
    analyser.smoothingTimeConstant = 0.2;
    var dataArrayLength = analyser.frequencyBinCount;
    var dataArray = new Uint8Array(dataArrayLength);
    source.connect(analyser);

    return setInterval(function() {
      analyser.getByteFrequencyData(dataArray);
      var sum = dataArray.reduce(function(prev, curr) { return prev + curr; }, 0);
      var level = sum / (255 * dataArrayLength);
      soundLevelSpanTag.style.paddingRight = level * 30 + "px";
    }, 100);
  }

  if (navigator.getUserMedia)
    window.initRecorder = function(startBtn, pauseBtn, stopBtn, replayTag, soundLevelTag) {
      startBtn.click(function() {
        navigator.getUserMedia({ audio: true }, startRecording, function(e) { alert(e) });
      });
      pauseBtn.click(function() {
        if (! recorder)
          return;
        if (isRecording) {
          recorder.pause();
          pauseBtn.text("▶");
        } else {
          recorder.resume();
          pauseBtn.text("▮▮");
        }
        isRecording = ! isRecording;
      });
      stopBtn.click(stopRecording);
      replayAudioTag = replayTag[0];
      soundLevelSpanTag = soundLevelTag[0];
    };
  else
    alert("audio not supported");

  window.postWithRecord = function(url, data) {
    var fd = new FormData();
    for (var key in data)
      fd.append(key, data[key]);
    if (recordData)
      fd.set("record", recordData);
    recordData = undefined;
    replayAudioTag.style.display = "none";
    return $.ajax({
      type: "POST",
      url: url,
      data: fd,
      processData: false,
      contentType: false
    });
  };
})();

(function() {
  function newSocket(messageCallback, closeCallback) {
    var socket = new WebSocket("ws://" + window.location.host + "/api/v2/chat/history");
    socket.ready = false;
    socket.onopen = function() {
      socket.ready = true;
    };
    socket.onclose = function(e) {
      socket.ready = false;
      console.log("socket closed: " + e.code + " " + e.reason);
      setTimeout(closeCallback, 1000);
    };
    socket.onmessage = messageCallback;
    return socket;
  }
  function onChatMessage(message) {
    var history = JSON.parse(message.data);
    if (! history.length)
      return;
    var chat = $("#chat");
    var scrollDown = chat[0].scrollHeight - chat[0].clientHeight == chat[0].scrollTop;
    history.forEach(function(msg) {
      var chatEntry = $('<p><b>' + msg.author + '</b> (' + msg.date + '): ' + msg.text + '</p>');
      chatEntry.appendTo(chat);
      if (msg.audio) {
        var playBtn = $('<button type="button" class="btn btn-default btn-sm play-btn">play</button>');
        playBtn.appendTo(chatEntry).click(function() {
          $.ajax({
            url: "api/v1/audio",
            data: { id: msg.audio },
            xhrFields: { responseType: "arraybuffer" }
          }).done(function(data) {
            playBtn.hide();
            createAudio(data).appendTo(chatEntry);
          }).fail(function(response) {
            alert(response.responseText);
          });
        });
      }
    });
    if (scrollDown)
      chat.animate({ scrollTop: chat[0].scrollHeight }, { duration: 1000, queue: false });
  }

  var chatSocket;

  window.connectToChat = function() {
    chatSocket = newSocket(onChatMessage, connectToChat);
  };
  window.refreshHistory = function() {
    if (! chatSocket.ready)
      return;
    chatSocket.send(JSON.stringify({ chatId: activeChatId }));
  };
})();