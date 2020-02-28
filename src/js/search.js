---

---
$(document).ready(function() {
    buildSearch();
  });

function buildSearch(){
  const urlParams = new URLSearchParams(window.location.search);
  const query = urlParams.get('term');
  var resultdiv = $('#results');      
  var result = index.search(query);
  resultdiv.empty();

  resultdiv.prepend('<h1>Search results for <b>'+query+'</b></h1>');
  var list = '<div class="result"><ul>';
  for (var item in result) {
    var ref = result[item].ref;
    list += '<li class="result-item"><h3><a href="/rest.li'+store[ref].link+'" class="post-title">'+store[ref].title+'</a></h3><p class="markdown-body">'
    if(store[ref].desc != null) {
       list +=  store[ref].desc;
    }
    list += '</p></li>';
  }
  list += '</ul></div>';
  resultdiv.append(list);

  document.getElementById("search-input").value = query
}
