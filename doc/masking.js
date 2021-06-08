function copy_to_clipbord(text) {
    var input = document.createElement('textarea');
    input.innerHTML = text;
    document.body.appendChild(input);
    input.select();
    var result = document.execCommand('copy');
    document.body.removeChild(input);
    return result;
}

for (let element of document.getElementsByTagName("pw")) {
    element.onclick = function() {copy_to_clipbord(this.getElementsByTagName("hd")[0].textContent);};
}
