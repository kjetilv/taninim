import taninim.yellin.server.ServerYellin;

import static com.github.kjetilv.uplift.asynchttp.MainSupport.parameterMap;

void main(String[] args) {
    new ServerYellin(parameterMap(args)).run();
}