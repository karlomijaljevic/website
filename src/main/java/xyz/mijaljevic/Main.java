package xyz.mijaljevic;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Main class of the application.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@QuarkusMain
public final class Main
{
	// TODO: Write some simple test cases for the website.

	public static final void main(String... args)
	{
		Quarkus.run(Website.class, args);
	}
}
