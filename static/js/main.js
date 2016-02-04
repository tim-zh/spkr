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
	var record;
	var isRecording = false;
	var replay;

	function startRecording(stream) {
		//todo show sound level
	  recorder = new MediaStreamRecorder(stream);
	  recorder.mimeType = 'audio/ogg';
	  recorder.audioChannels = 1;
	  recorder.stream = stream;
	  recorder.ondataavailable = function(blob) {
		  replay.src = URL.createObjectURL(blob);
		  replay.style.display = "inline";
			record = blob;
	  };

	  recorder.start(140 * 1000);
    isRecording = true;
	}

	window.initRecorder = function(startBtn, pauseBtn, stopBtn, replayTag) {
  	startBtn.click(function() {
  		navigator.mediaDevices.getUserMedia({ audio: true }).then(startRecording).catch(function(e) { alert(e) });
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
  	stopBtn.click(function() {
  	  if (! recorder)
  	    return;
	    isRecording = false;
  		recorder.stop();
  		recorder.stream.stop();
  	});
		replay = replayTag[0];
  };

	window.postWithRecord = function(url, data) {
		var fd = new FormData();
		for (var key in data)
			fd.append(key, data[key]);
    fd.set("record", record);
    return $.ajax({
      type: "POST",
      url: url,
      data: fd,
      processData: false,
      contentType: false
    });
	};
})();
