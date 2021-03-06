<!doctype html>
<!--suppress HtmlUnknownTarget -->
<HTML lang="en">
<HEAD>
    <LINK HREF="/res/taninim.css" REL="stylesheet">
    <LINK HREF="/res/plyr.css" REL="stylesheet">
    <META charset="utf-8">
    <TITLE>taninim: admin</TITLE>
</HEAD>
<BODY>

<H1>Sesjoner: {length(sessions)}</H1>

{sessions:{session|
<H3>{session.fbUser.name} {session.cookie}</H3>
<FORM ACTION="/admin/exterminate" METHOD="POST">
    <INPUT NAME="session" TYPE="hidden" VALUE="{session.cookie}">
    <UL>
        <LI>Access level: {session.accessLevel}</LI>
        <LI>Status: {session.currentStatus}</LI>
        <LI>Login time: {session.startTime}</LI>
        <LI>Streamed: {session.prettyStreamedBytes}/{session.prettyStreamQuota}</LI>
        <LI>
            <BUTTON TYPE="submit" value="Close">Close</BUTTON>
        </LI>
    </UL>
    <P><CODE>{session.description}</CODE></P>
</FORM>
}}

<H1>ids.json</H1>

<FORM ACTION="/admin/ids" METHOD="POST">
    <LABEL>
        <TEXTAREA NAME="updatedIds" STYLE="width: 90%; height: 256px">{ids}</TEXTAREA>
    </LABEL>
    <BR>
    <BUTTON TYPE="submit" value="Upload">Upload</BUTTON>
</FORM>
</BODY>
</HTML>
