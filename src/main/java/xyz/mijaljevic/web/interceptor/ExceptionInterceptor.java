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

package xyz.mijaljevic.web.interceptor;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;

/**
 * Intercepts exceptions that occur on the website and displays them on the
 * error page. This is a safety net, in case something goes wrong and an
 * exception is thrown that is not handled anywhere else.
 */
@Provider
public final class ExceptionInterceptor implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(final Exception exception) {
        return Response.seeOther(URI.create("/error/exception")).build();
    }
}
