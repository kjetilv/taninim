import module java.base;
import taninim.yellin.server.ServerYellin;

import static com.github.kjetilv.uplift.util.MainSupport.parameterMap;

void main(String[] args) {
    new ServerYellin(parameterMap(args)).run();
}