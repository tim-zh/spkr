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
    return $.ajax({
      type: "POST",
      url: url,
      data: fd,
      processData: false,
      contentType: false
    });
	};
})();
