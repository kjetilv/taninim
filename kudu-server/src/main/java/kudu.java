import taninim.kudu.server.Parameters;
import taninim.kudu.server.ServerKudu;

void main(String[] args) {
    new ServerKudu(Parameters.parse(args)).run();
}