package org.votaciones;

import ReportsSystem.ReportsServicePrx;
import com.zeroc.Ice.*;
import com.zeroc.Ice.Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AutomatedCacheTest {

    private static final Logger logger = LoggerFactory.getLogger(AutomatedCacheTest.class);

    private static final String[] CITIZEN_IDS = {
            "15050547", "15050548", "15050549", "15050550", "15050551", "15050552", "15050553", "15050554", "15050555", "15050556",
            "15050557", "15050558", "15050559", "15050560", "15050561", "15050562", "15050563", "15050564", "15050565", "15050566",
            "15050567", "15050568", "15050569", "15050570", "15050571", "15050572", "15050573", "15050574", "15050575", "15050576",
            "15050577", "15050578", "15050579", "15050580", "15050581", "15050582", "15050583", "15050584", "15050585", "15050586",
            "15050587", "15050588", "15050589", "15050590", "15050591", "15050592", "15050593", "15050594", "15050595", "15050596",
            "15050597", "15050598", "15050599", "15050600", "15050601", "15050602", "15050603", "15050604", "15050605", "15050606",
            "15050607", "15050608", "15050609", "15050610", "15050611", "15050612", "15050613", "15050614", "15050615", "15050616",
            "15050617", "15050618", "15050619", "15050620", "15050621", "15050622", "15050623", "15050624", "15050625", "15050626",
            "15050627", "15050628", "15050629", "15050630", "15050631", "15050632", "15050633", "15050634", "15050635", "15050636",
            "15050637", "15050638", "15050639", "15050640", "15050641", "15050642", "15050643", "15050644", "15050645", "15050646",
            "15050647", "15050648", "15050649", "15050650", "15050651", "15050652", "15050653", "15050654", "15050655", "15050656",
            "15050657", "15050658", "15050659", "15050660", "15050661", "15050662", "15050663", "15050664", "15050665", "15050666",
            "15050667", "15050668", "15050669", "15050670", "15050671", "15050672", "15050673", "15050674", "15050675", "15050676",
            "15050677", "15050678", "15050679", "15050680", "15050681", "15050682", "15050683", "15050684", "15050685", "15050686",
            "15050687", "15050688", "15050689", "15050690", "15050691", "15050692", "15050693", "15050694", "15050695", "15050696",
            "15050697", "15050698", "15050699", "15050700", "15050701", "15050702", "15050703", "15050704", "15050705", "15050706",
            "15050707", "15050708", "15050709", "15050710", "15050711", "15050712", "15050713", "15050714", "15050715", "15050716",
            "15050717", "15050718", "15050719", "15050720", "15050721", "15050722", "15050723", "15050724", "15050725", "15050726",
            "15050727", "15050728", "15050729", "15050730", "15050731", "15050732", "15050733", "15050734", "15050735", "15050736",
            "15050737", "15050738", "15050739", "15050740", "15050741", "15050742", "15050743", "15050744", "15050745", "15050746",
            "15050747", "15050748", "15050749", "15050750", "15050751", "15050752", "15050753", "15050754", "15050755", "15050756",
            "15050757", "15050758", "15050759", "15050760", "15050761", "15050762", "15050763", "15050764", "15050765", "15050766",
            "15050767", "15050768", "15050769", "15050770", "15050771", "15050772", "15050773", "15050774", "15050775", "15050776",
            "15050777", "15050778", "15050779", "15050780", "15050781", "15050782", "15050783", "15050784", "15050785", "15050786",
            "15050787", "15050788", "15050789", "15050790", "15050791", "15050792", "15050793", "15050794", "15050795", "15050796",
            "15050797", "15050798", "15050799", "15050800", "15050801", "15050802", "15050803", "15050804", "15050805", "15050806",
            "15050807", "15050808", "15050809", "15050810", "15050811", "15050812", "15050813", "15050814", "15050815", "15050816",
            "15050817", "15050818", "15050819", "15050820", "15050821", "15050822", "15050823", "15050824", "15050825", "15050826",
            "15050827", "15050828", "15050829", "15050830", "15050831", "15050832", "15050833", "15050834", "15050835", "15050836",
            "15050837", "15050838", "15050839", "15050840", "15050841", "15050842", "15050843", "15050844", "15050845", "15050846",
            "15050847", "15050848", "15050849", "15050850", "15050851", "15050852", "15050853", "15050854", "15050855", "15050856",
            "15050857", "15050858", "15050859", "15050860", "15050861", "15050862", "15050863", "15050864", "15050865", "15050866",
            "15050867", "15050868", "15050869", "15050870", "15050871", "15050872", "15050873", "15050874", "15050875", "15050876",
            "15050877", "15050878", "15050879", "15050880", "15050881", "15050882", "15050883", "15050884", "15050885", "15050886",
            "15050887", "15050888", "15050889", "15050890", "15050891", "15050892", "15050893", "15050894", "15050895", "15050896",
            "15050897", "15050898", "15050899", "15050900", "15050901", "15050902", "15050903", "15050904", "15050905", "15050906",
            "15050907", "15050908", "15050909", "15050910", "15050911", "15050912", "15050913", "15050914", "15050915", "15050916",
            "15050917", "15050918", "15050919", "15050920", "15050921", "15050922", "15050923", "15050924", "15050925", "15050926",
            "15050927", "15050928", "15050929", "15050930", "15050931", "15050932", "15050933", "15050934", "15050935", "15050936",
            "15050937", "15050938", "15050939", "15050940", "15050941", "15050942", "15050943", "15050944", "15050945", "15050946",
            "15050947", "15050948", "15050949", "15050950", "15050951", "15050952", "15050953", "15050954", "15050955", "15050956",
            "15050957", "15050958", "15050959", "15050960", "15050961", "15050962", "15050963", "15050964", "15050965", "15050966",
            "15050967", "15050968", "15050969", "15050970", "15050971", "15050972", "15050973", "15050974", "15050975", "15050976",
            "15050977", "15050978", "15050979", "15050980", "15050981", "15050982", "15050983", "15050984", "15050985", "15050986",
            "15050987", "15050988", "15050989", "15050990", "15050991", "15050992", "15050993", "15050994", "15050995", "15050996",
            "15050997", "15050998", "15050999", "15051000", "15051001", "15051002", "15051003", "15051004", "15051005", "15051006",
            "15051007", "15051008", "15051009", "15051010", "15051011", "15051012", "15051013", "15051014", "15051015", "15051016",
            "15051017", "15051018", "15051019", "15051020", "15051021", "15051022", "15051023", "15051024", "15051025", "15051026",
            "15051027", "15051028", "15051029", "15051030", "15051031", "15051032", "15051033", "15051034", "15051035", "15051036",
            "15051037", "15051038", "15051039", "15051040", "15051041", "15051042", "15051043", "15051044", "15051045", "15051046",
            "15051047", "15051048", "15051049", "15051050", "15051051", "15051052", "15051053", "15051054", "15051055", "15051056",
            "15051057", "15051058", "15051059", "15051060", "15051061", "15051062", "15051063", "15051064", "15051065", "15051066",
            "15051067", "15051068", "15051069", "15051070", "15051071", "15051072", "15051073", "15051074", "15051075", "15051076",
            "15051077", "15051078", "15051079", "15051080", "15051081", "15051082", "15051083", "15051084", "15051085", "15051086",
            "15051087", "15051088", "15051089", "15051090", "15051091", "15051092", "15051093", "15051094", "15051095", "15051096",
            "15051097", "15051098", "15051099", "15051100", "15051101", "15051102", "15051103", "15051104", "15051105", "15051106",
            "15051107", "15051108", "15051109", "15051110", "15051111", "15051112", "15051113", "15051114", "15051115", "15051116",
            "15051117", "15051118", "15051119", "15051120", "15051121", "15051122", "15051123", "15051124", "15051125", "15051126",
            "15051127", "15051128", "15051129", "15051130", "15051131", "15051132", "15051133", "15051134", "15051135", "15051136",
            "15051137", "15051138", "15051139", "15051140", "15051141", "15051142", "15051143", "15051144", "15051145", "15051146",
            "15051147", "15051148", "15051149", "15051150", "15051151", "15051152", "15051153", "15051154", "15051155", "15051156",
            "15051157", "15051158", "15051159", "15051160", "15051161", "15051162", "15051163", "15051164", "15051165", "15051166",
            "15051167", "15051168", "15051169", "15051170", "15051171", "15051172", "15051173", "15051174", "15051175", "15051176",
            "15051177", "15051178", "15051179", "15051180", "15051181", "15051182", "15051183", "15051184", "15051185", "15051186",
            "15051187", "15051188", "15051189", "15051190", "15051191", "15051192", "15051193", "15051194", "15051195", "15051196",
            "15051197", "15051198", "15051199", "15051200", "15051201", "15051202", "15051203", "15051204", "15051205", "15051206",
            "15051207", "15051208", "15051209", "15051210", "15051211", "15051212", "15051213", "15051214", "15051215", "15051216",
            "15051217", "15051218", "15051219", "15051220", "15051221", "15051222", "15051223", "15051224", "15051225", "15051226",
            "15051227", "15051228", "15051229", "15051230", "15051231", "15051232", "15051233", "15051234", "15051235", "15051236",
            "15051237", "15051238", "15051239", "15051240", "15051241", "15051242", "15051243", "15051244", "15051245", "15051246",
            "15051247", "15051248", "15051249", "15051250", "15051251", "15051252", "15051253", "15051254", "15051255", "15051256",
            "15051257", "15051258", "15051259", "15051260", "15051261", "15051262", "15051263", "15051264", "15051265", "15051266",
            "15051267", "15051268", "15051269", "15051270", "15051271", "15051272", "15051273", "15051274", "15051275", "15051276",
            "15051277", "15051278", "15051279", "15051280", "15051281", "15051282", "15051283", "15051284", "15051285", "15051286",
            "15051287", "15051288", "15051289", "15051290", "15051291", "15051292", "15051293", "15051294", "15051295", "15051296",
            "15051297", "15051298", "15051299", "15051300", "15051301", "15051302", "15051303", "15051304", "15051305", "15051306",
            "15051307", "15051308", "15051309", "15051310", "15051311", "15051312", "15051313", "15051314", "15051315", "15051316",
            "15051317", "15051318", "15051319", "15051320", "15051321", "15051322", "15051323", "15051324", "15051325", "15051326",
            "15051327", "15051328", "15051329", "15051330", "15051331", "15051332", "15051333", "15051334", "15051335", "15051336",
            "15051337", "15051338", "15051339", "15051340", "15051341", "15051342", "15051343", "15051344", "15051345", "15051346",
            "15051347", "15051348", "15051349", "15051350", "15051351", "15051352", "15051353", "15051354", "15051355", "15051356",
            "15051357", "15051358", "15051359", "15051360", "15051361", "15051362", "15051363", "15051364", "15051365", "15051366",
            "15051367", "15051368", "15051369", "15051370", "15051371", "15051372", "15051373", "15051374", "15051375", "15051376",
            "15051377", "15051378", "15051379", "15051380", "15051381", "15051382", "15051383", "15051384", "15051385", "15051386",
            "15051387", "15051388", "15051389", "15051390", "15051391", "15051392", "15051393", "15051394", "15051395", "15051396",
            "15051397", "15051398", "15051399", "15051400", "15051401", "15051402", "15051403", "15051404", "15051405", "15051406",
            "15051407", "15051408", "15051409", "15051410", "15051411", "15051412", "15051413", "15051414", "15051415", "15051416",
            "15051417", "15051418", "15051419", "15051420", "15051421", "15051422", "15051423", "15051424", "15051425", "15051426",
            "15051427", "15051428", "15051429", "15051430", "15051431", "15051432", "15051433", "15051434", "15051435", "15051436",
            "15051437", "15051438", "15051439", "15051440", "15051441", "15051442", "15051443", "15051444", "15051445", "15051446",
            "15051447", "15051448", "15051449", "15051450", "15051451", "15051452", "15051453", "15051454", "15051455", "15051456",
            "15051457", "15051458", "15051459", "15051460", "15051461", "15051462", "15051463", "15051464", "15051465", "15051466",
            "15051467", "15051468", "15051469", "15051470", "15051471", "15051472", "15051473", "15051474", "15051475", "15051476",
            "15051477", "15051478", "15051479", "15051480", "15051481", "15051482", "15051483", "15051484", "15051485", "15051486",
            "15051487", "15051488", "15051489", "15051490", "15051491", "15051492", "15051493", "15051494", "15051495", "15051496",
            "15051497", "15051498", "15051499", "15051500", "15051501", "15051502", "15051503", "15051504", "15051505", "15051506",
            "15051507", "15051508", "15051509", "15051510", "15051511", "15051512", "15051513", "15051514", "15051515", "15051516",
            "15051517", "15051518", "15051519", "15051520", "15051521", "15051522", "15051523", "15051524", "15051525", "15051526",
            "15051527", "15051528", "15051529", "15051530", "15051531", "15051532", "15051533", "15051534", "15051535", "15051536",
            "15051537", "15051538", "15051539", "15051540", "15051541", "15051542", "15051543", "15051544", "15051545", "15051546"
    };

    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {

            System.out.println("========== PRUEBA AUTOMÁTICA CACHE INTELIGENTE ==========");
            System.out.println("Ejecutando escenarios para activar precarga predictiva");
            System.out.println("Ciudadanos disponibles para prueba: " + CITIZEN_IDS.length);

            ObjectPrx base = communicator.stringToProxy("ProxyCacheReports:default -h localhost -p 9999");
            ReportsServicePrx proxyCache = ReportsServicePrx.checkedCast(base);

            if (proxyCache == null) {
                System.err.println("Error: No se pudo conectar al proxy cache");
                return;
            }

            System.out.println("Conectado al proxy cache");
            System.out.println("Iniciando pruebas automáticas...\n");

            AutomatedCacheTest test = new AutomatedCacheTest();

            test.showInitialStats(proxyCache);
            test.runHotspotScenario(proxyCache);
            test.waitForAnalysis("primera ronda de hotspots");
            test.runRandomScenario(proxyCache);
            test.waitForAnalysis("consultas aleatorias");
            test.runHighLoadScenario(proxyCache);
            test.showFinalStats(proxyCache);

            System.out.println("\n========== PRUEBA AUTOMÁTICA COMPLETADA ==========");
            System.out.println("Revisa los logs del servidor para confirmar:");
            System.out.println("   - Detección de hotspots");
            System.out.println("   - Ejecución de precarga predictiva");
            System.out.println("   - Mejora en hit rate");

        } catch (Exception e) {
            System.err.println("Error en prueba automática: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showInitialStats(ReportsServicePrx proxyCache) {
        try {
            System.out.println("========== ESTADÍSTICAS INICIALES ==========");
            String stats = proxyCache.getCacheStats();
            String[] lines = stats.split("\n");
            for (String line : lines) {
                if (line.contains("Total consultas") ||
                        line.contains("Cache hits") ||
                        line.contains("Hit rate") ||
                        line.contains("Patrones de consulta activos")) {
                    System.out.println("   " + line);
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas iniciales: " + e.getMessage());
        }
    }

    private void runHotspotScenario(ReportsServicePrx proxyCache) {
        System.out.println("========== ESCENARIO 1: SIMULACIÓN DE HOTSPOT ==========");
        System.out.println("Consultando los primeros 50 ciudadanos 4 veces cada uno");

        int electionId = 1;
        long totalTime = 0;
        int totalQueries = 0;

        for (int round = 1; round <= 4; round++) {
            System.out.println("\nRonda " + round + " de consultas intensivas:");

            for (int i = 0; i < 50; i++) {
                try {
                    String documento = CITIZEN_IDS[i];
                    long startTime = System.currentTimeMillis();

                    String result = proxyCache.getCitizenReports(documento, electionId);

                    long endTime = System.currentTimeMillis();
                    long queryTime = endTime - startTime;
                    totalTime += queryTime;
                    totalQueries++;

                    if (i % 10 == 0) {
                        System.out.println(" Consulta " + (i + 1) + ": " + documento + " (" + queryTime + " ms)");
                    }

                    Thread.sleep(100);

                } catch (Exception | InterruptedException e) {
                    System.err.println("Error en consulta " + CITIZEN_IDS[i] + ": " + e.getMessage());
                }
            }

            System.out.println("Ronda " + round + " completada - Promedio: " +
                    (totalTime / totalQueries) + " ms/consulta");
        }

        System.out.println("Escenario hotspot completado: " + totalQueries + " consultas en " + totalTime + " ms");
    }

    private void runRandomScenario(ReportsServicePrx proxyCache) {
        System.out.println("\n========== ESCENARIO 2: CONSULTAS ALEATORIAS ==========");
        System.out.println("Consultando 100 ciudadanos aleatorios para dispersar el cache");

        int electionId = 1;
        Random random = new Random();
        long totalTime = 0;

        for (int i = 0; i < 100; i++) {
            try {
                String documento = CITIZEN_IDS[random.nextInt(CITIZEN_IDS.length)];
                long startTime = System.currentTimeMillis();

                String result = proxyCache.getCitizenReports(documento, electionId);

                long endTime = System.currentTimeMillis();
                long queryTime = endTime - startTime;
                totalTime += queryTime;

                if (i % 25 == 0) {
                    System.out.println("Consulta aleatoria " + (i + 1) + ": " + documento + " (" + queryTime + " ms)");
                }

                Thread.sleep(50);

            } catch (Exception | InterruptedException e) {
                System.err.println("Error en consulta aleatoria: " + e.getMessage());
            }
        }

        System.out.println("Escenario aleatorio completado: 100 consultas en " + totalTime + " ms");
    }

    private void runHighLoadScenario(ReportsServicePrx proxyCache) {
        System.out.println("\n========== ESCENARIO 3: CARGA ALTA ==========");
        System.out.println("Simulando carga alta con consultas rápidas concentradas");

        int electionId = 1;
        long totalTime = 0;
        int totalQueries = 0;

        for (int round = 1; round <= 3; round++) {
            System.out.println("\nRonda rápida " + round + ":");

            for (int i = 100; i < 150; i++) {
                try {
                    String documento = CITIZEN_IDS[i];
                    long startTime = System.currentTimeMillis();

                    String result = proxyCache.getCitizenReports(documento, electionId);

                    long endTime = System.currentTimeMillis();
                    long queryTime = endTime - startTime;
                    totalTime += queryTime;
                    totalQueries++;

                    if (i % 20 == 0) {
                        System.out.println("Consulta rápida " + (i + 1) + ": " + documento + " (" + queryTime + " ms)");
                    }


                } catch (Exception e) {
                    System.err.println("Error en carga alta: " + e.getMessage());
                }
            }
        }

        System.out.println("Escenario carga alta completado: " + totalQueries + " consultas en " + totalTime + " ms");
    }

    private void waitForAnalysis(String description) {
        try {
            System.out.println("\nEsperando 35 segundos para análisis automático después de " + description + "...");
            System.out.println("Durante esta pausa, el sistema debería:");
            System.out.println("   - Analizar patrones de consulta");
            System.out.println("   - Detectar hotspots");
            System.out.println("   - Ejecutar precarga predictiva");

            for (int i = 35; i > 0; i--) {
                System.out.print("\r" + i + " segundos restantes...");
                Thread.sleep(1000);
            }
            System.out.println("\rPausa de análisis completada");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void showFinalStats(ReportsServicePrx proxyCache) {
        try {
            System.out.println("\n========== ESTADÍSTICAS FINALES ==========");
            String stats = proxyCache.getCacheStats();

            System.out.println(stats);

            String[] lines = stats.split("\n");
            System.out.println("\n========== RESUMEN DE RESULTADOS ==========");

            for (String line : lines) {
                if (line.contains("Total consultas") ||
                        line.contains("Precarga predictiva") ||
                        line.contains("Cache hits") ||
                        line.contains("Cache misses") ||
                        line.contains("Hit rate") ||
                        line.contains("Patrones de consulta activos") ||
                        line.contains("Memoria total utilizada")) {
                    System.out.println("   " + line.trim());
                }
            }

            // Buscar hotspots detectados
            boolean foundHotspots = false;
            System.out.println("\nHotspots detectados:");
            for (String line : lines) {
                if (line.contains(" ") || line.contains("hotspot")) {
                    System.out.println("   " + line.trim());
                    foundHotspots = true;
                }
            }

            if (!foundHotspots) {
                System.out.println("No se detectaron hotspots en las estadísticas");
                System.out.println("Revisa los logs del servidor para confirmar detección");
            }

        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas finales: " + e.getMessage());
        }
    }

    private void runMesaHotspotTest(ReportsServicePrx proxyCache) {
        System.out.println("\n========== PRUEBA ESPECÍFICA: HOTSPOT DE MESA ==========");

        try {
            int mesaId = 34203; 
            int electionId = 1;

            System.out.println("Generando hotspot para mesa " + mesaId);

            for (int i = 0; i < 5; i++) {
                long startTime = System.currentTimeMillis();
                String[] results = proxyCache.getMesaCitizenReports(mesaId, electionId);
                long endTime = System.currentTimeMillis();

                System.out.println("Consulta mesa " + (i + 1) + ": " +
                        results.length + " ciudadanos (" + (endTime - startTime) + " ms)");

                Thread.sleep(500);
            }

        } catch (Exception e) {
            System.err.println("Error en prueba de mesa: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void runGeographicHotspotTest(ReportsServicePrx proxyCache) {
        System.out.println("\n========== PRUEBA ESPECÍFICA: HOTSPOT GEOGRÁFICO ==========");

        try {
            int locationId = 1;
            int electionId = 1;
            String[] locationTypes = {"municipality", "puesto", "department"};

            for (String locationType : locationTypes) {
                System.out.println("Generando hotspot para " + locationType + " " + locationId);

                for (int i = 0; i < 4; i++) {
                    long startTime = System.currentTimeMillis();
                    String result = proxyCache.getGeographicReports(locationId, locationType, electionId);
                    long endTime = System.currentTimeMillis();

                    System.out.println("Consulta " + locationType + " " + (i + 1) + ": " +
                            (endTime - startTime) + " ms");

                    Thread.sleep(300);
                }
            }

        } catch (Exception | InterruptedException e) {
            System.err.println("Error en prueba geográfica: " + e.getMessage());
        }
    }

    public static void runExtendedTests(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {

            System.out.println("========== PRUEBAS EXTENDIDAS CACHE INTELIGENTE ==========");

            ObjectPrx base = communicator.stringToProxy("ProxyCacheReports:default -h localhost -p 9999");
            ReportsServicePrx proxyCache = ReportsServicePrx.checkedCast(base);

            if (proxyCache == null) {
                System.err.println("Error: No se pudo conectar al proxy cache");
                return;
            }

            System.out.println("Conectado al proxy cache");

            AutomatedCacheTest test = new AutomatedCacheTest();

            test.showInitialStats(proxyCache);
            test.runHotspotScenario(proxyCache);
            test.waitForAnalysis("hotspot de ciudadanos");

            test.runMesaHotspotTest(proxyCache);
            test.waitForAnalysis("hotspot de mesa");

            test.runGeographicHotspotTest(proxyCache);
            test.waitForAnalysis("hotspot geográfico");

            test.runRandomScenario(proxyCache);
            test.runHighLoadScenario(proxyCache);
            test.waitForAnalysis("carga final");

            test.showFinalStats(proxyCache);

            System.out.println("\n========== TODAS LAS PRUEBAS COMPLETADAS ==========");

        } catch (Exception e) {
            System.err.println("Error en pruebas extendidas: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void runPerformanceComparison(ReportsServicePrx proxyCache) {
        System.out.println("\n========== COMPARACIÓN DE RENDIMIENTO ==========");

        int electionId = 1;
        List<Long> coldTimes = new ArrayList<>();
        List<Long> warmTimes = new ArrayList<>();

        // Consultas en frío (primeras consultas)
        System.out.println("Midiendo rendimiento en frío...");
        for (int i = 500; i < 520; i++) {
            try {
                String documento = CITIZEN_IDS[i];
                long startTime = System.currentTimeMillis();
                proxyCache.getCitizenReports(documento, electionId);
                long endTime = System.currentTimeMillis();
                coldTimes.add(endTime - startTime);
                Thread.sleep(100);
            } catch (Exception | InterruptedException e) {
                System.err.println("Error en consulta fría: " + e.getMessage());
            }
        }

        System.out.println("Midiendo rendimiento en caliente...");
        for (int i = 500; i < 520; i++) {
            try {
                String documento = CITIZEN_IDS[i];
                long startTime = System.currentTimeMillis();
                proxyCache.getCitizenReports(documento, electionId);
                long endTime = System.currentTimeMillis();
                warmTimes.add(endTime - startTime);
                Thread.sleep(50);
            } catch (Exception | InterruptedException e) {
                System.err.println("Error en consulta caliente: " + e.getMessage());
            }
        }

        double coldAvg = coldTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double warmAvg = warmTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double improvement = ((coldAvg - warmAvg) / coldAvg) * 100;

        System.out.println("Resultados de rendimiento:");
        System.out.println("   - Promedio en frío: " + String.format("%.1f", coldAvg) + " ms");
        System.out.println("   - Promedio en caliente: " + String.format("%.1f", warmAvg) + " ms");
        System.out.println("   - Mejora: " + String.format("%.1f", improvement) + "%");
    }
}