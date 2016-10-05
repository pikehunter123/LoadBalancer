package ru.zyv;

//import org.apache.camel.main.Main;

import java.io.File;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import org.apache.camel.main.Main;
import org.apache.commons.io.FileUtils;
import sun.nio.ch.IOUtil;


class MyProcessor implements Processor {
  @Override
  public void process(Exchange exchange) throws Exception {
      String cn = exchange.getIn().getClass().getCanonicalName();
      System.out.println("message class="+cn);
      //
      org.apache.camel.component.file.GenericFileMessage fm=(org.apache.camel.component.file.GenericFileMessage) exchange.getIn();
      String bn=fm.getBody().getClass().getCanonicalName();
      System.out.println("body class="+bn);
      org.apache.camel.component.file.GenericFile gf=(org.apache.camel.component.file.GenericFile) fm.getBody();
      
              String s=FileUtils.readFileToString(new File(gf.getBody().toString()), "utf8");
              System.out.println("message body="+s);
              
              
      
      Thread.currentThread().sleep(10000);
      
    // do something...
  }
}
/**
 * A Camel Application
 */

class SimulateHttpError implements Processor {

    @Override
    public void process(Exchange exchng) throws Exception {
        throw new ConnectException("Simulated connection error");
    }

}

public class MainApp {

    static final int N = 100;

    private static String getHostName() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    /**
     * A main() so we can easily run these routing rules in our IDE
     */
    public static void main(String... args) throws Exception {
        Main main = new Main();
        main.enableHangupSupport();
        main.addRouteBuilder(new MyRouteBuilder());
        main.run(args);
        System.exit(0);
        
        final CountDownLatch latch = new CountDownLatch(N);
        final CamelContext context = new DefaultCamelContext();

        final String hostName = getHostName();

//        ProducerTemplate template = context.createProducerTemplate();

//        ExecutorServiceStrategy strategy = context.getExecutorServiceStrategy();
//        ThreadPoolProfile profile = strategy.getDefaultThreadPoolProfile();
//        profile.setMaxPoolSize(10);
//
//        ThreadPoolProfile custom = new ThreadPoolProfileSupport("myPool");
//        custom.setMaxPoolSize(10);
//        context.getExecutorServiceStrategy().registerThreadPoolProfile(custom);
//        context.addRoutes(new RouteBuilder() {
//
//                              @Override
//                              public void configure() throws Exception {
//                                  from("jetty:http://localhost:8080/early").routeId("input")
//                                          .wireTap("direct:incoming")
//                                          .transform().constant("OK");
//                                  from("direct:incoming").routeId("process")
//                                          .convertBodyTo(String.class)
//                                          .to("stream:out")
//                                          .log("Incoming ${body}")
//                                          .delay(3000)
//                                          .log("Processing done for ${body}")
//                                          .to("mock:result");
//                              }
//                          }
//        );

        context.addRoutes(
                new RouteBuilder() {
                    @Override
                    public void configure() throws UnknownHostException {
//                        context.setTracing(Boolean.TRUE);

                        fromF("jetty:http://%s/t320/services/HelloService?matchOnUriPrefix=true", hostName).routeId("mainRoute")
                                .loadBalance().failover(1, true, true)
                                .to("direct:a").to("direct:b")
                                .end();

                        from("direct:a")
                                //                        .to("metrics:counter:simple.countera")
                                //                        .to("metrics:timer:simple.timer?action=start")
                                .choice()
                                .when(header("CamelHttpQuery").isEqualTo("wsdl"))
                                .to("direct:e")
                                .otherwise()
                                .to("direct:f");
//                        .to("metrics:timer:simple.timer?action=stop");

                        from("direct:b")
                                //                        .to("metrics:counter:simple.counterb")
                                .choice()
                                .when(header("CamelHttpQuery").isEqualTo("wsdl"))
                                .to("direct:c")
                                .otherwise()
                                .to("direct:d");

                        from("direct:c")
                                .to("http://t320webservices.open.ac.uk/t320/services/HelloService?bridgeEndpoint=true")
                                .streamCaching()
                                .transform(body().regexReplaceAll("t320webservices.open.ac.uk", hostName))
                                .to("log:DIRECT C");

                        from("direct:d")
                                //                        .process(new SimulateHttpError())
                                .to("http://t320webservices.open.ac.uk/t320/services/HelloService?bridgeEndpoint=true")
                                .to("log:START DIRECT D")
//                        .delay(2000)
                                .to("log:END DIRECT D");

                        from("direct:e")
                                .to("http://t320webservices.open.ac.uk/t320/services/HelloService?bridgeEndpoint=true")
                                .streamCaching()
                                .transform(body().regexReplaceAll("t320webservices.open.ac.uk", hostName))
                                .to("log:DIRECT E");

                        from("direct:f")
                                .to("http://t320webservices.open.ac.uk/t320/services/HelloService?bridgeEndpoint=true")
                                .to("log:START DIRECT F")
//                        .delay(2000)
                                .to("log:END DIRECT F");
                    }
                }
        );

//        context.addRoutePolicyFactory(
//                new MetricsRoutePolicyFactory());

//        RouteDefinition route = context.getRouteDefinitions().get(0);
//        route.adviceWith(context, new RouteBuilder() {
//
//            @Override
//            public void configure() throws Exception {
//                interceptSendToEndpoint("direct:e")
//                        .skipSendToOriginalEndpoint()
//                        .process(new SimulateHttpError());
//            }
//        });

        context.start();

//        String message = "      <nks:helloName xmlns:nks=\"http://nks34.t320\">\n"
//                + "         <nks:name>Yuri</nks:name>\n"
//                + "      </nks:helloName>";
//        String message = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:nks=\"http://nks34.t320\">\n"
//                + "   <soapenv:Header/>\n"
//                + "   <soapenv:Body>\n"
//                + "      <nks:helloName>\n"
//                + "         <!--Optional:-->\n"
//                + "         <nks:name>Polina</nks:name>\n"
//                + "      </nks:helloName>\n"
//                + "   </soapenv:Body>\n"
//                + "</soapenv:Envelope>";

//        for (int i = 0; i < N; i++) {
//            template.requestBody("direct:example", message);
//        }
//        Object resp = template.requestBody("direct:example", message);
//        System.out.println("\nresp: " + resp.toString());
        latch.await();

        context.stop();
    }

}

