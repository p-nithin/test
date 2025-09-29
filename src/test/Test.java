package test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Test {

	final static boolean traceResult = false;
	final static List<String> testResult = new ArrayList<String>();

	public static void main(String[] args) {
		testTypesense();
	}

	private static List<Set<String>> getRandomOptions(final List<String> options, final int size) {
		final var ret = new ArrayList<Set<String>>();
		final var x = new ArrayList<String>(options);
		for (int i = 0; i <= size; i++) {
			Collections.shuffle(x);
			final var y = new HashSet<>(x.subList(0, new Random().nextInt(x.size() - 1) + 1));
			if (!ret.contains(y))
				ret.add(y);
		}
		return ret;
	}

	private static void testTypesense() {
		final var propertyTypes = getRandomOptions(List.of("Commercial", "Condo/Townhouse", "Farm", "Land",
				"Mobile/Manufactured", "Multi-Family", "Rental", "Single Family"), 25);
		final var interiorFeatures = getRandomOptions(List.of("Walk-In Closet(s)", "Ceiling Fan(s)", "Pantry",
				"Eat-in Kitchen", "Entrance Foyer", "Kitchen Island", "Main Level Master", "Separate Shower", "Attic",
				"High Ceilings", "Washer/Dryer Hookup", "Cable TV", "Living/Dining Room", "Open Floorplan",
				"Pull Down Attic Stairs", "Bedroom on Main Level", "Built-in Features", "Tray Ceiling(s)",
				"Window Treatments", "Breakfast Bar", "Granite Counters", "Double Vanity", "Fireplace", "Elevator",
				"First Floor Entry", "Walk-In Shower", "Breakfast Area", "Garage Door Opener", "Smoke Detector(s)",
				"Cathedral Ceiling(s)", "Counters - Laminate", "Recessed Lighting", "New Paint", "High Speed Internet",
				"Counters - Granite", "Split Bedrooms", "Eat-in Bar", "Bookcases", "Separate/Formal Dining Room",
				"Other", "Jetted Tub", "Tub Shower", "Kitchen/Family Room Combo", "Closet Cabinetry",
				"Upper Level Master", "Vaulted Ceiling(s)", "Master Downstairs", "Solid Surface Counters", "Foyer",
				"Utility Room"), 50);
		final var locations = List.of("26.918213,-82.219616", "26.999703,-82.212275", "34.093831,-118.381697",
				"37.106902,-94.497647", "34.178105,-115.642899", "26.816433,-81.993812", "29.939406,-81.508166",
				"29.693556,-98.072850", "29.880163,-97.966164", "29.935364,-95.744725", "31.631426,-106.160065",
				"34.713656,-117.864660", "34.585234,-117.453724", "36.860533,-94.401675", "25.985129,-80.147261",
				"34.100517,-118.414630", "34.066117,-118.846342", "29.517748,-81.230696", "26.649280,-81.616279",
				"32.336832,-80.847794", "37.182918,-94.477775", "34.571218,-117.133073", "37.021691,-94.510899",
				"35.544010,-78.829824", "29.889085,-98.244624", "33.815151,-83.893561", "25.933997,-80.135801",
				"35.986719,-78.554351", "30.562170,-97.907802", "32.163171,-80.755765", "25.766206,-80.182897",
				"29.853215,-95.831456", "37.189255,-94.279255", "31.829712,-105.971363", "26.027459,-80.120219",
				"32.219462,-80.898538", "26.693201,-81.945126", "27.051628,-82.114523", "26.562706,-81.601925",
				"29.758817,-98.191068", "33.570930,-117.773043", "32.238247,-80.740263", "29.542743,-97.937075",
				"34.747390,-118.369249", "26.692897,-82.034065", "29.581507,-81.218196", "40.539413,-111.321274",
				"25.779391,-80.151566", "32.226824,-81.057442", "33.652403,-117.373758");
		runTest(1000, () -> {
			final var filter_by = String.format("PropertyType:=[`%s`]&&InteriorFeatures:=[`%s`]&&Location:(%s,%d mi)",
					String.join("`,`", propertyTypes.get(new Random().nextInt(propertyTypes.size()))),
					String.join("`,`", interiorFeatures.get(new Random().nextInt(interiorFeatures.size()))),
					locations.get(new Random().nextInt(locations.size())), new Random().nextInt(25) + 10);
			String res;
			try {
				res = getHttpResponseString(
						"https://typesense-service.idcrealestate.com/collections/rog_cmncmn/documents/search?q=*&per_page=0&filter_by="
								+ URLEncoder.encode(filter_by, StandardCharsets.UTF_8.toString()),
						new String[] { "X-TYPESENSE-API-KEY", "QeXpxALG8lbBnJQAMkzLb2m7Hvd4TrBD" });
				if (Test.traceResult) {
					Pattern pattern = Pattern.compile("(\"found\":)(\\d+)");
					Matcher matcher = pattern.matcher(res);
					while (matcher.find()) {
						Test.testResult
								.add(String.format("Total count: %s, filter_by: %s", matcher.group(2), filter_by));
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		});
	}

	private static String getHttpResponseString(final String url, final String... headers) {
		try {
			HttpClient client = HttpClient.newBuilder().build();
			HttpRequest request = HttpRequest.newBuilder().headers(headers).uri(URI.create(url)).GET().build();
			String body = client.send(request, BodyHandlers.ofString()).body();
			return body;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void runTest(final int numOfRuns, Runnable test) {
		final var counterSuccess = new AtomicInteger(0);
		final var counterFailure = new AtomicInteger(0);
		final var startTime = System.nanoTime();
		final List<Thread> threadPool = new ArrayList<>();
		IntStream.range(0, numOfRuns).parallel().forEach(j -> {
			final var thread = new Thread(() -> {
				try {
					test.run();
					counterSuccess.addAndGet(1);
				} catch (final Exception e) {
					System.out.println(e.getMessage());
					counterFailure.addAndGet(1);
				}
			});
			thread.start();
			threadPool.add(thread);
		});
		for (final Thread thread : threadPool) {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(String.format("Running test %s times takes %.02f seconds, SUCCESS: %s, FAILURE: %s",
				numOfRuns, (System.nanoTime() - startTime) / 1e9f, counterSuccess, counterFailure));
		if (Test.traceResult)
			Test.testResult.forEach(System.out::println);
	}

}
