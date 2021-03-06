/*
 * Copyright 2020 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aiven.elasticsearch.repositories.metadata;

import javax.crypto.SecretKey;

import java.io.IOException;
import java.util.Base64;

import io.aiven.elasticsearch.repositories.security.EncryptionKeyProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EncryptedRepositoryMetadata {

    private final EncryptionKeyProvider encryptionKeyProvider;

    private final ObjectMapper objectMapper;

    static final int VERSION = 1;

    public EncryptedRepositoryMetadata(final EncryptionKeyProvider encryptionKeyProvider) {
        this.encryptionKeyProvider = encryptionKeyProvider;
        this.objectMapper = new ObjectMapper();
    }

    public byte[] serialize(final SecretKey encryptionKey) throws IOException {
        final var encryptedKey =
            Base64.getEncoder()
                .encodeToString(encryptionKeyProvider.encryptKey(encryptionKey));
        return objectMapper.writeValueAsBytes(new EncryptionKeyMetadata(encryptedKey, VERSION));
    }

    public SecretKey deserialize(final byte[] metadata) throws IOException {
        final EncryptionKeyMetadata encryptionKeyMetadata;
        try {
            encryptionKeyMetadata = objectMapper.readValue(metadata, EncryptionKeyMetadata.class);
        } catch (final Exception e) {
            throw new IOException("Couldn't read JSON metadata", e);
        }
        if (encryptionKeyMetadata.version() != VERSION) {
            throw new IOException("Unsupported metadata version");
        }
        final var encryptedKey =
            Base64.getDecoder().decode(encryptionKeyMetadata.key());
        return encryptionKeyProvider.decryptKey(encryptedKey);
    }

}
