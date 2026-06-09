/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */

package xyz.mijaljevic.domain.rss;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

/**
 * Represents the <i>atom:link</i> element in the RSS XML file that the website
 * serves for RSS readers.
 */
@Data
@SuppressWarnings("unused")
@XmlRootElement(
        name = "link",
        namespace = "https://www.w3.org/2005/Atom"
)
public final class AtomLink {
    /**
     * Value of the <i>href</i> attribute.
     */
    private String href;

    /**
     * Value of the <i>rel</i> attribute.
     */
    private String rel;

    /**
     * Value of the <i>type</i> attribute.
     */
    private String type;

    /**
     * @return The value of the <i>href</i> attribute.
     */
    @XmlAttribute(name = "href")
    public String getHref() {
        return href;
    }

    /**
     * @return The value of the <i>rel</i> attribute.
     */
    @XmlAttribute(name = "rel")
    public String getRel() {
        return rel;
    }

    /**
     * @return The value of the <i>type</i> attribute.
     */
    @XmlAttribute(name = "type")
    public String getType() {
        return type;
    }
}
