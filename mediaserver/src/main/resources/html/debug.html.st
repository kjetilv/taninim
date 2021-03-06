<!doctype html>
<!--suppress HtmlUnknownTarget -->
<HTML lang="en">
<HEAD>
    <LINK HREF="/res/taninim.css" REL="stylesheet">
    <LINK HREF="/res/plyr.css" REL="stylesheet">
    <META charset="utf-8">
    <TITLE>taninim: debug</TITLE>
</HEAD>
<BODY>

<H1>Sessions: {length(exchanges)}</H1>

<OL>
    {exchanges:{entry|
    <UL>
        <A HREF="#{entry.key}">{entry.key}</A>
    </UL>
    }}
</OL>

{exchanges:{entry|
<H4 ID="{entry.key}">{entry.key}</H4>

{entry.value:{exchange|
<H5>{exchange.sequenceNo} => {exchange.handler} @ {exchange.time}</H5>

<PRE>
{exchange.request}

{exchange.response}

{exchange.sessionStatus}
</PRE>
}}
}}

<H1>ids.json</H1>

<FORM ACTION="/admin" METHOD="POST">
    <LABEL>
        <TEXTAREA NAME="updatedIds" STYLE="width: 90%; height: 256px">{ids}</TEXTAREA>
    </LABEL>
    <BR>
    <BUTTON TYPE="submit" value="Upload">Upload</BUTTON>
</FORM>
</BODY>
</HTML>
