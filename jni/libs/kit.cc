/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string>
#include <vector>

#include "crypto/p224_spake.h"
#include "src/data_encoding.h"
#include "src/privet/openssl_utils.h"

#include "kit.h"


bool create_auth_code(std::string password, std::string sessionId, std::string devCmt,
		std::string &cliCmt, std::string &authCode)
{
    crypto::P224EncryptedKeyExchange spake{
        crypto::P224EncryptedKeyExchange::kPeerTypeClient, password};

	cliCmt = weave::Base64Encode(spake.GetNextMessage());

	std::vector<uint8_t> deviceCommitmentDecoded;
	weave::Base64Decode(devCmt, &deviceCommitmentDecoded);

	spake.ProcessMessage(std::string(deviceCommitmentDecoded.begin(),
		deviceCommitmentDecoded.end()));

	const std::string &key = spake.GetUnverifiedKey();
	std::vector<uint8_t> authCodeRaw{
		weave::privet::HmacSha256(std::vector<uint8_t>{key.begin(), key.end()},
			std::vector<uint8_t>{sessionId.begin(), sessionId.end()})};

	authCode = weave::Base64Encode(authCodeRaw);

	return true;
}
