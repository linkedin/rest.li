# tomd awk pre-processor (before pandoc)
# redirects {% highlight %} sections to numbered files
# and passes through <notextile> blocks

BEGIN { nme = "tomd-include-"; cnt = 0; }

/{% +highlight/ {
  including = "yes";
  incfile = (nme cnt++ ".txt");
  print ("!include " incfile);
}

/{% +endhighlight/ {
  including = "";
  print >incfile;
  next;
}

/^<notextile>/ {
  including = "yes";
  incfile = (nme cnt++ ".txt");
  print ("!include " incfile);
  next;
}

/^<\/notextile>/ {
  including = "";
  next;
}

{ if(including) { print >incfile } else { print }}