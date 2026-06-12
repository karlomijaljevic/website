package xyz.mijaljevic.domain.entity;

/**
 * Defines the static file types that the website serves. Currently, it
 * only serves (excluding blog HTML pages):
 * <ul>
 *      <li>CSS</li>
 *      <li>JS</li>
 *      <li>IMAGE</li>
 * </ul>
 */
public enum StaticFileType {
    /**
     * A CSS stylesheet.
     */
    CSS,
    /**
     * A JavaScript file.
     */
    JS,
    /**
     * An image file.
     */
    IMAGE
}
