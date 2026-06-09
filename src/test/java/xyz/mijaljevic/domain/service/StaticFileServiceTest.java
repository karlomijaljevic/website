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
package xyz.mijaljevic.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.domain.entity.StaticFile.Type;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticFileServiceTest {
    @Mock
    EntityManager em;

    @Mock
    TypedQuery<StaticFile> query;

    private StaticFileService service;

    @BeforeEach
    void setUp() {
        service = new StaticFileService(em);
    }

    @Test
    @DisplayName("findFileByName returns the single matching static file")
    void findFileByName_match_returnsFile() {
        StaticFile staticFile = new StaticFile();
        staticFile.setName("style.css");
        when(em.createQuery(anyString(), eq(StaticFile.class))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(staticFile);

        assertThat(service.findFileByName("style.css")).isSameAs(staticFile);
    }

    @Test
    @DisplayName("findFileByName swallows NoResultException and returns null")
    void findFileByName_noResult_returnsNull() {
        when(em.createQuery(anyString(), eq(StaticFile.class))).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertThat(service.findFileByName("missing.css")).isNull();
    }

    @Test
    @DisplayName("listAllMissingFiles returns the query result list")
    void listAllMissingFiles_returnsResultList() {
        StaticFile staticFile = new StaticFile();
        when(em.createQuery(anyString(), eq(StaticFile.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(staticFile));

        assertThat(service.listAllMissingFiles(List.of("kept.css"), Type.CSS))
                .containsExactly(staticFile);
    }
}
