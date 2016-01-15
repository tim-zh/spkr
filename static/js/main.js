function getSessionCookie(key) {
	var value = Cookies.get("ssession")
	if (! value)
		return undefined;
	value = value.substring(value.indexOf("-") + 1);
	return value.split("&").map(function(x) {
		return x.split("=");
	}).filter(function(x) {
		return x[0] == key;
	})[0][1];
}