<!DOCTYPE html>
<!--suppress HtmlUnknownTarget -->
<HTML lang="no">
<HEAD>
    <LINK REL="stylesheet" HREF="/res/taninim.css">
    <META charset="UTF-8">
    <TITLE>taninim</TITLE>
</HEAD>

<BODY>

<SCRIPT SRC="/res/facebook.js">
</SCRIPT>

<DIV id="fb-root"></DIV>

<SCRIPT async defer crossorigin="anonymous"
        src="https://connect.facebook.net/nn_NO/sdk.js#xfbml=1&version=v5.0&appId=398404340829160&autoLogAppEvents=1">
</SCRIPT>

<SCRIPT SRC="/res/connectfb.js">
</SCRIPT>

<DIV STYLE="float: right">
    <svg version="1.0" xmlns="http://www.w3.org/2000/svg"
         width="50%" height="50%" viewBox="0 0 200 200">
        <g transform="translate(-20,220) scale(0.12,-0.12)" fill="#70A070">
            <path d="M889 1595 l-112 -195 -228 0 c-126 0 -229 -3 -229 -6 0 -4 49 -93
    110 -199 61 -106 110 -195 110 -198 0 -3 -51 -93 -113 -200 l-114 -194 81 -7
    c45 -3 127 -6 182 -6 l101 0 259 437 c143 240 258 442 256 447 -8 24 -177 316
    -183 316 -4 0 -58 -88 -120 -195z"/>
            <path d="M1074 963 c-143 -240 -258 -442 -256 -447 8 -24 177 -316 183 -316 4
    0 58 88 120 195 l112 195 228 0 c126 0 229 3 229 6 0 4 -49 93 -110 199 -60
    106 -110 195 -110 198 0 3 51 93 113 200 l114 194 -90 7 c-50 3 -132 6 -182 6
    l-92 0 -259 -437z"/>
        </g>
    </svg>
</DIV>

<DIV style="float:left"><!--suppress XmlUnboundNsPrefix, HtmlUnknownTag -->
    <fb:login-button scope="public_profile" onlogin="fbLogin();">
    </fb:login-button>
</DIV>

</BODY>
</HTML>
