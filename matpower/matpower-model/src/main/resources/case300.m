function mpc = case300
%IEEE300CDF
%    05/13/91 CYME INTERNATIONAL    100.0 1991 S IEEE 300-BUS TEST SYSTEM
%
%   Converted by MATPOWER 7.0 using CDF2MPC on 11-Mar-2020
%   from 'ieee300cdf.txt'.
%
%   WARNINGS:
%       check the title format in the first line of the cdf file.
%       negative Pg at bus 8 treated as Pd
%       negative Pg at bus 10 treated as Pd
%       negative Pg at bus 20 treated as Pd
%       negative Pg at bus 138 treated as Pd
%       Qmax = Qmin at generator at bus 7049 (Qmax set to Qmin + 10)
%       negative Pg at bus 9002 treated as Pd
%       negative Pg at bus 9051 treated as Pd
%       negative Pg at bus 9053 treated as Pd
%       Insufficient generation, setting Pmax at slack bus (bus 7049) to 2399.01
%       MVA limit of branch 37 - 9001 not given, set to 0
%       MVA limit of branch 9001 - 9005 not given, set to 0
%       MVA limit of branch 9001 - 9006 not given, set to 0
%       MVA limit of branch 9001 - 9012 not given, set to 0
%       MVA limit of branch 9005 - 9051 not given, set to 0
%       MVA limit of branch 9005 - 9052 not given, set to 0
%       MVA limit of branch 9005 - 9053 not given, set to 0
%       MVA limit of branch 9005 - 9054 not given, set to 0
%       MVA limit of branch 9005 - 9055 not given, set to 0
%       MVA limit of branch 9006 - 9007 not given, set to 0
%       MVA limit of branch 9006 - 9003 not given, set to 0
%       MVA limit of branch 9006 - 9003 not given, set to 0
%       MVA limit of branch 9012 - 9002 not given, set to 0
%       MVA limit of branch 9012 - 9002 not given, set to 0
%       MVA limit of branch 9002 - 9021 not given, set to 0
%       MVA limit of branch 9021 - 9023 not given, set to 0
%       MVA limit of branch 9021 - 9022 not given, set to 0
%       MVA limit of branch 9002 - 9024 not given, set to 0
%       MVA limit of branch 9023 - 9025 not given, set to 0
%       MVA limit of branch 9023 - 9026 not given, set to 0
%       MVA limit of branch 9007 - 9071 not given, set to 0
%       MVA limit of branch 9007 - 9072 not given, set to 0
%       MVA limit of branch 9007 - 9003 not given, set to 0
%       MVA limit of branch 9003 - 9031 not given, set to 0
%       MVA limit of branch 9003 - 9032 not given, set to 0
%       MVA limit of branch 9003 - 9033 not given, set to 0
%       MVA limit of branch 9003 - 9044 not given, set to 0
%       MVA limit of branch 9044 - 9004 not given, set to 0
%       MVA limit of branch 9004 - 9041 not given, set to 0
%       MVA limit of branch 9004 - 9042 not given, set to 0
%       MVA limit of branch 9004 - 9043 not given, set to 0
%       MVA limit of branch 9003 - 9034 not given, set to 0
%       MVA limit of branch 9003 - 9035 not given, set to 0
%       MVA limit of branch 9003 - 9036 not given, set to 0
%       MVA limit of branch 9003 - 9037 not given, set to 0
%       MVA limit of branch 9003 - 9038 not given, set to 0
%       MVA limit of branch 9012 - 9121 not given, set to 0
%       MVA limit of branch 9053 - 9533 not given, set to 0
%       MVA limit of branch 1 - 5 not given, set to 0
%       MVA limit of branch 2 - 6 not given, set to 0
%       MVA limit of branch 2 - 8 not given, set to 0
%       MVA limit of branch 3 - 7 not given, set to 0
%       MVA limit of branch 3 - 19 not given, set to 0
%       MVA limit of branch 3 - 150 not given, set to 0
%       MVA limit of branch 4 - 16 not given, set to 0
%       MVA limit of branch 5 - 9 not given, set to 0
%       MVA limit of branch 7 - 12 not given, set to 0
%       MVA limit of branch 7 - 131 not given, set to 0
%       MVA limit of branch 8 - 11 not given, set to 0
%       MVA limit of branch 8 - 14 not given, set to 0
%       MVA limit of branch 9 - 11 not given, set to 0
%       MVA limit of branch 11 - 13 not given, set to 0
%       MVA limit of branch 12 - 21 not given, set to 0
%       MVA limit of branch 13 - 20 not given, set to 0
%       MVA limit of branch 14 - 15 not given, set to 0
%       MVA limit of branch 15 - 37 not given, set to 0
%       MVA limit of branch 15 - 89 not given, set to 0
%       MVA limit of branch 15 - 90 not given, set to 0
%       MVA limit of branch 16 - 42 not given, set to 0
%       MVA limit of branch 19 - 21 not given, set to 0
%       MVA limit of branch 19 - 87 not given, set to 0
%       MVA limit of branch 20 - 22 not given, set to 0
%       MVA limit of branch 20 - 27 not given, set to 0
%       MVA limit of branch 21 - 24 not given, set to 0
%       MVA limit of branch 22 - 23 not given, set to 0
%       MVA limit of branch 23 - 25 not given, set to 0
%       MVA limit of branch 24 - 319 not given, set to 0
%       MVA limit of branch 25 - 26 not given, set to 0
%       MVA limit of branch 26 - 27 not given, set to 0
%       MVA limit of branch 26 - 320 not given, set to 0
%       MVA limit of branch 33 - 34 not given, set to 0
%       MVA limit of branch 33 - 38 not given, set to 0
%       MVA limit of branch 33 - 40 not given, set to 0
%       MVA limit of branch 33 - 41 not given, set to 0
%       MVA limit of branch 34 - 42 not given, set to 0
%       MVA limit of branch 35 - 72 not given, set to 0
%       MVA limit of branch 35 - 76 not given, set to 0
%       MVA limit of branch 35 - 77 not given, set to 0
%       MVA limit of branch 36 - 88 not given, set to 0
%       MVA limit of branch 37 - 38 not given, set to 0
%       MVA limit of branch 37 - 40 not given, set to 0
%       MVA limit of branch 37 - 41 not given, set to 0
%       MVA limit of branch 37 - 49 not given, set to 0
%       MVA limit of branch 37 - 89 not given, set to 0
%       MVA limit of branch 37 - 90 not given, set to 0
%       MVA limit of branch 38 - 41 not given, set to 0
%       MVA limit of branch 38 - 43 not given, set to 0
%       MVA limit of branch 39 - 42 not given, set to 0
%       MVA limit of branch 40 - 48 not given, set to 0
%       MVA limit of branch 41 - 42 not given, set to 0
%       MVA limit of branch 41 - 49 not given, set to 0
%       MVA limit of branch 41 - 51 not given, set to 0
%       MVA limit of branch 42 - 46 not given, set to 0
%       MVA limit of branch 43 - 44 not given, set to 0
%       MVA limit of branch 43 - 48 not given, set to 0
%       MVA limit of branch 43 - 53 not given, set to 0
%       MVA limit of branch 44 - 47 not given, set to 0
%       MVA limit of branch 44 - 54 not given, set to 0
%       MVA limit of branch 45 - 60 not given, set to 0
%       MVA limit of branch 45 - 74 not given, set to 0
%       MVA limit of branch 46 - 81 not given, set to 0
%       MVA limit of branch 47 - 73 not given, set to 0
%       MVA limit of branch 47 - 113 not given, set to 0
%       MVA limit of branch 48 - 107 not given, set to 0
%       MVA limit of branch 49 - 51 not given, set to 0
%       MVA limit of branch 51 - 52 not given, set to 0
%       MVA limit of branch 52 - 55 not given, set to 0
%       MVA limit of branch 53 - 54 not given, set to 0
%       MVA limit of branch 54 - 55 not given, set to 0
%       MVA limit of branch 55 - 57 not given, set to 0
%       MVA limit of branch 57 - 58 not given, set to 0
%       MVA limit of branch 57 - 63 not given, set to 0
%       MVA limit of branch 58 - 59 not given, set to 0
%       MVA limit of branch 59 - 61 not given, set to 0
%       MVA limit of branch 60 - 62 not given, set to 0
%       MVA limit of branch 62 - 64 not given, set to 0
%       MVA limit of branch 62 - 144 not given, set to 0
%       MVA limit of branch 63 - 526 not given, set to 0
%       MVA limit of branch 69 - 211 not given, set to 0
%       MVA limit of branch 69 - 79 not given, set to 0
%       MVA limit of branch 70 - 71 not given, set to 0
%       MVA limit of branch 70 - 528 not given, set to 0
%       MVA limit of branch 71 - 72 not given, set to 0
%       MVA limit of branch 71 - 73 not given, set to 0
%       MVA limit of branch 72 - 77 not given, set to 0
%       MVA limit of branch 72 - 531 not given, set to 0
%       MVA limit of branch 73 - 76 not given, set to 0
%       MVA limit of branch 73 - 79 not given, set to 0
%       MVA limit of branch 74 - 88 not given, set to 0
%       MVA limit of branch 74 - 562 not given, set to 0
%       MVA limit of branch 76 - 77 not given, set to 0
%       MVA limit of branch 77 - 78 not given, set to 0
%       MVA limit of branch 77 - 80 not given, set to 0
%       MVA limit of branch 77 - 552 not given, set to 0
%       MVA limit of branch 77 - 609 not given, set to 0
%       MVA limit of branch 78 - 79 not given, set to 0
%       MVA limit of branch 78 - 84 not given, set to 0
%       MVA limit of branch 79 - 211 not given, set to 0
%       MVA limit of branch 80 - 211 not given, set to 0
%       MVA limit of branch 81 - 194 not given, set to 0
%       MVA limit of branch 81 - 195 not given, set to 0
%       MVA limit of branch 85 - 86 not given, set to 0
%       MVA limit of branch 86 - 87 not given, set to 0
%       MVA limit of branch 86 - 323 not given, set to 0
%       MVA limit of branch 89 - 91 not given, set to 0
%       MVA limit of branch 90 - 92 not given, set to 0
%       MVA limit of branch 91 - 94 not given, set to 0
%       MVA limit of branch 91 - 97 not given, set to 0
%       MVA limit of branch 92 - 103 not given, set to 0
%       MVA limit of branch 92 - 105 not given, set to 0
%       MVA limit of branch 94 - 97 not given, set to 0
%       MVA limit of branch 97 - 100 not given, set to 0
%       MVA limit of branch 97 - 102 not given, set to 0
%       MVA limit of branch 97 - 103 not given, set to 0
%       MVA limit of branch 98 - 100 not given, set to 0
%       MVA limit of branch 98 - 102 not given, set to 0
%       MVA limit of branch 99 - 107 not given, set to 0
%       MVA limit of branch 99 - 108 not given, set to 0
%       MVA limit of branch 99 - 109 not given, set to 0
%       MVA limit of branch 99 - 110 not given, set to 0
%       MVA limit of branch 100 - 102 not given, set to 0
%       MVA limit of branch 102 - 104 not given, set to 0
%       MVA limit of branch 103 - 105 not given, set to 0
%       MVA limit of branch 104 - 108 not given, set to 0
%       MVA limit of branch 104 - 322 not given, set to 0
%       MVA limit of branch 105 - 107 not given, set to 0
%       MVA limit of branch 105 - 110 not given, set to 0
%       MVA limit of branch 108 - 324 not given, set to 0
%       MVA limit of branch 109 - 110 not given, set to 0
%       MVA limit of branch 109 - 113 not given, set to 0
%       MVA limit of branch 109 - 114 not given, set to 0
%       MVA limit of branch 110 - 112 not given, set to 0
%       MVA limit of branch 112 - 114 not given, set to 0
%       MVA limit of branch 115 - 122 not given, set to 0
%       MVA limit of branch 116 - 120 not given, set to 0
%       MVA limit of branch 117 - 118 not given, set to 0
%       MVA limit of branch 118 - 119 not given, set to 0
%       MVA limit of branch 118 - 1201 not given, set to 0
%       MVA limit of branch 1201 - 120 not given, set to 0
%       MVA limit of branch 118 - 121 not given, set to 0
%       MVA limit of branch 119 - 120 not given, set to 0
%       MVA limit of branch 119 - 121 not given, set to 0
%       MVA limit of branch 122 - 123 not given, set to 0
%       MVA limit of branch 122 - 125 not given, set to 0
%       MVA limit of branch 123 - 124 not given, set to 0
%       MVA limit of branch 123 - 125 not given, set to 0
%       MVA limit of branch 125 - 126 not given, set to 0
%       MVA limit of branch 126 - 127 not given, set to 0
%       MVA limit of branch 126 - 129 not given, set to 0
%       MVA limit of branch 126 - 132 not given, set to 0
%       MVA limit of branch 126 - 157 not given, set to 0
%       MVA limit of branch 126 - 158 not given, set to 0
%       MVA limit of branch 126 - 169 not given, set to 0
%       MVA limit of branch 127 - 128 not given, set to 0
%       MVA limit of branch 127 - 134 not given, set to 0
%       MVA limit of branch 127 - 168 not given, set to 0
%       MVA limit of branch 128 - 130 not given, set to 0
%       MVA limit of branch 128 - 133 not given, set to 0
%       MVA limit of branch 129 - 130 not given, set to 0
%       MVA limit of branch 129 - 133 not given, set to 0
%       MVA limit of branch 130 - 132 not given, set to 0
%       MVA limit of branch 130 - 151 not given, set to 0
%       MVA limit of branch 130 - 167 not given, set to 0
%       MVA limit of branch 130 - 168 not given, set to 0
%       MVA limit of branch 133 - 137 not given, set to 0
%       MVA limit of branch 133 - 168 not given, set to 0
%       MVA limit of branch 133 - 169 not given, set to 0
%       MVA limit of branch 133 - 171 not given, set to 0
%       MVA limit of branch 134 - 135 not given, set to 0
%       MVA limit of branch 134 - 184 not given, set to 0
%       MVA limit of branch 135 - 136 not given, set to 0
%       MVA limit of branch 136 - 137 not given, set to 0
%       MVA limit of branch 136 - 152 not given, set to 0
%       MVA limit of branch 137 - 140 not given, set to 0
%       MVA limit of branch 137 - 181 not given, set to 0
%       MVA limit of branch 137 - 186 not given, set to 0
%       MVA limit of branch 137 - 188 not given, set to 0
%       MVA limit of branch 139 - 172 not given, set to 0
%       MVA limit of branch 140 - 141 not given, set to 0
%       MVA limit of branch 140 - 142 not given, set to 0
%       MVA limit of branch 140 - 145 not given, set to 0
%       MVA limit of branch 140 - 146 not given, set to 0
%       MVA limit of branch 140 - 147 not given, set to 0
%       MVA limit of branch 140 - 182 not given, set to 0
%       MVA limit of branch 141 - 146 not given, set to 0
%       MVA limit of branch 142 - 143 not given, set to 0
%       MVA limit of branch 143 - 145 not given, set to 0
%       MVA limit of branch 143 - 149 not given, set to 0
%       MVA limit of branch 145 - 146 not given, set to 0
%       MVA limit of branch 145 - 149 not given, set to 0
%       MVA limit of branch 146 - 147 not given, set to 0
%       MVA limit of branch 148 - 178 not given, set to 0
%       MVA limit of branch 148 - 179 not given, set to 0
%       MVA limit of branch 152 - 153 not given, set to 0
%       MVA limit of branch 153 - 161 not given, set to 0
%       MVA limit of branch 154 - 156 not given, set to 0
%       MVA limit of branch 154 - 183 not given, set to 0
%       MVA limit of branch 155 - 161 not given, set to 0
%       MVA limit of branch 157 - 159 not given, set to 0
%       MVA limit of branch 158 - 159 not given, set to 0
%       MVA limit of branch 158 - 160 not given, set to 0
%       MVA limit of branch 162 - 164 not given, set to 0
%       MVA limit of branch 162 - 165 not given, set to 0
%       MVA limit of branch 163 - 164 not given, set to 0
%       MVA limit of branch 165 - 166 not given, set to 0
%       MVA limit of branch 167 - 169 not given, set to 0
%       MVA limit of branch 172 - 173 not given, set to 0
%       MVA limit of branch 172 - 174 not given, set to 0
%       MVA limit of branch 173 - 174 not given, set to 0
%       MVA limit of branch 173 - 175 not given, set to 0
%       MVA limit of branch 173 - 176 not given, set to 0
%       MVA limit of branch 175 - 176 not given, set to 0
%       MVA limit of branch 175 - 179 not given, set to 0
%       MVA limit of branch 176 - 177 not given, set to 0
%       MVA limit of branch 177 - 178 not given, set to 0
%       MVA limit of branch 178 - 179 not given, set to 0
%       MVA limit of branch 178 - 180 not given, set to 0
%       MVA limit of branch 181 - 138 not given, set to 0
%       MVA limit of branch 181 - 187 not given, set to 0
%       MVA limit of branch 184 - 185 not given, set to 0
%       MVA limit of branch 186 - 188 not given, set to 0
%       MVA limit of branch 187 - 188 not given, set to 0
%       MVA limit of branch 188 - 138 not given, set to 0
%       MVA limit of branch 189 - 208 not given, set to 0
%       MVA limit of branch 189 - 209 not given, set to 0
%       MVA limit of branch 190 - 231 not given, set to 0
%       MVA limit of branch 190 - 240 not given, set to 0
%       MVA limit of branch 191 - 192 not given, set to 0
%       MVA limit of branch 192 - 225 not given, set to 0
%       MVA limit of branch 193 - 205 not given, set to 0
%       MVA limit of branch 193 - 208 not given, set to 0
%       MVA limit of branch 194 - 219 not given, set to 0
%       MVA limit of branch 194 - 664 not given, set to 0
%       MVA limit of branch 195 - 219 not given, set to 0
%       MVA limit of branch 196 - 197 not given, set to 0
%       MVA limit of branch 196 - 210 not given, set to 0
%       MVA limit of branch 197 - 198 not given, set to 0
%       MVA limit of branch 197 - 211 not given, set to 0
%       MVA limit of branch 198 - 202 not given, set to 0
%       MVA limit of branch 198 - 203 not given, set to 0
%       MVA limit of branch 198 - 210 not given, set to 0
%       MVA limit of branch 198 - 211 not given, set to 0
%       MVA limit of branch 199 - 200 not given, set to 0
%       MVA limit of branch 199 - 210 not given, set to 0
%       MVA limit of branch 200 - 210 not given, set to 0
%       MVA limit of branch 201 - 204 not given, set to 0
%       MVA limit of branch 203 - 211 not given, set to 0
%       MVA limit of branch 204 - 205 not given, set to 0
%       MVA limit of branch 205 - 206 not given, set to 0
%       MVA limit of branch 206 - 207 not given, set to 0
%       MVA limit of branch 206 - 208 not given, set to 0
%       MVA limit of branch 212 - 215 not given, set to 0
%       MVA limit of branch 213 - 214 not given, set to 0
%       MVA limit of branch 214 - 215 not given, set to 0
%       MVA limit of branch 214 - 242 not given, set to 0
%       MVA limit of branch 215 - 216 not given, set to 0
%       MVA limit of branch 216 - 217 not given, set to 0
%       MVA limit of branch 217 - 218 not given, set to 0
%       MVA limit of branch 217 - 219 not given, set to 0
%       MVA limit of branch 217 - 220 not given, set to 0
%       MVA limit of branch 219 - 237 not given, set to 0
%       MVA limit of branch 220 - 218 not given, set to 0
%       MVA limit of branch 220 - 221 not given, set to 0
%       MVA limit of branch 220 - 238 not given, set to 0
%       MVA limit of branch 221 - 223 not given, set to 0
%       MVA limit of branch 222 - 237 not given, set to 0
%       MVA limit of branch 224 - 225 not given, set to 0
%       MVA limit of branch 224 - 226 not given, set to 0
%       MVA limit of branch 225 - 191 not given, set to 0
%       MVA limit of branch 226 - 231 not given, set to 0
%       MVA limit of branch 227 - 231 not given, set to 0
%       MVA limit of branch 228 - 229 not given, set to 0
%       MVA limit of branch 228 - 231 not given, set to 0
%       MVA limit of branch 228 - 234 not given, set to 0
%       MVA limit of branch 229 - 190 not given, set to 0
%       MVA limit of branch 231 - 232 not given, set to 0
%       MVA limit of branch 231 - 237 not given, set to 0
%       MVA limit of branch 232 - 233 not given, set to 0
%       MVA limit of branch 234 - 235 not given, set to 0
%       MVA limit of branch 234 - 237 not given, set to 0
%       MVA limit of branch 235 - 238 not given, set to 0
%       MVA limit of branch 241 - 237 not given, set to 0
%       MVA limit of branch 240 - 281 not given, set to 0
%       MVA limit of branch 242 - 245 not given, set to 0
%       MVA limit of branch 242 - 247 not given, set to 0
%       MVA limit of branch 243 - 244 not given, set to 0
%       MVA limit of branch 243 - 245 not given, set to 0
%       MVA limit of branch 244 - 246 not given, set to 0
%       MVA limit of branch 245 - 246 not given, set to 0
%       MVA limit of branch 245 - 247 not given, set to 0
%       MVA limit of branch 246 - 247 not given, set to 0
%       MVA limit of branch 247 - 248 not given, set to 0
%       MVA limit of branch 248 - 249 not given, set to 0
%       MVA limit of branch 249 - 250 not given, set to 0
%       MVA limit of branch 3 - 1 not given, set to 0
%       MVA limit of branch 3 - 2 not given, set to 0
%       MVA limit of branch 3 - 4 not given, set to 0
%       MVA limit of branch 7 - 5 not given, set to 0
%       MVA limit of branch 7 - 6 not given, set to 0
%       MVA limit of branch 10 - 11 not given, set to 0
%       MVA limit of branch 12 - 10 not given, set to 0
%       MVA limit of branch 15 - 17 not given, set to 0
%       MVA limit of branch 16 - 15 not given, set to 0
%       MVA limit of branch 21 - 20 not given, set to 0
%       MVA limit of branch 24 - 23 not given, set to 0
%       MVA limit of branch 36 - 35 not given, set to 0
%       MVA limit of branch 45 - 44 not given, set to 0
%       MVA limit of branch 45 - 46 not given, set to 0
%       MVA limit of branch 62 - 61 not given, set to 0
%       MVA limit of branch 63 - 64 not given, set to 0
%       MVA limit of branch 73 - 74 not given, set to 0
%       MVA limit of branch 81 - 88 not given, set to 0
%       MVA limit of branch 85 - 99 not given, set to 0
%       MVA limit of branch 86 - 102 not given, set to 0
%       MVA limit of branch 87 - 94 not given, set to 0
%       MVA limit of branch 114 - 207 not given, set to 0
%       MVA limit of branch 116 - 124 not given, set to 0
%       MVA limit of branch 121 - 115 not given, set to 0
%       MVA limit of branch 122 - 157 not given, set to 0
%       MVA limit of branch 130 - 131 not given, set to 0
%       MVA limit of branch 130 - 150 not given, set to 0
%       MVA limit of branch 132 - 170 not given, set to 0
%       MVA limit of branch 141 - 174 not given, set to 0
%       MVA limit of branch 142 - 175 not given, set to 0
%       MVA limit of branch 143 - 144 not given, set to 0
%       MVA limit of branch 143 - 148 not given, set to 0
%       MVA limit of branch 145 - 180 not given, set to 0
%       MVA limit of branch 151 - 170 not given, set to 0
%       MVA limit of branch 153 - 183 not given, set to 0
%       MVA limit of branch 155 - 156 not given, set to 0
%       MVA limit of branch 159 - 117 not given, set to 0
%       MVA limit of branch 160 - 124 not given, set to 0
%       MVA limit of branch 163 - 137 not given, set to 0
%       MVA limit of branch 164 - 155 not given, set to 0
%       MVA limit of branch 182 - 139 not given, set to 0
%       MVA limit of branch 189 - 210 not given, set to 0
%       MVA limit of branch 193 - 196 not given, set to 0
%       MVA limit of branch 195 - 212 not given, set to 0
%       MVA limit of branch 200 - 248 not given, set to 0
%       MVA limit of branch 201 - 69 not given, set to 0
%       MVA limit of branch 202 - 211 not given, set to 0
%       MVA limit of branch 204 - 2040 not given, set to 0
%       MVA limit of branch 209 - 198 not given, set to 0
%       MVA limit of branch 211 - 212 not given, set to 0
%       MVA limit of branch 218 - 219 not given, set to 0
%       MVA limit of branch 223 - 224 not given, set to 0
%       MVA limit of branch 229 - 230 not given, set to 0
%       MVA limit of branch 234 - 236 not given, set to 0
%       MVA limit of branch 238 - 239 not given, set to 0
%       MVA limit of branch 196 - 2040 not given, set to 0
%       MVA limit of branch 119 - 1190 not given, set to 0
%       MVA limit of branch 120 - 1200 not given, set to 0
%       MVA limit of branch 7002 - 2 not given, set to 0
%       MVA limit of branch 7003 - 3 not given, set to 0
%       MVA limit of branch 7061 - 61 not given, set to 0
%       MVA limit of branch 7062 - 62 not given, set to 0
%       MVA limit of branch 7166 - 166 not given, set to 0
%       MVA limit of branch 7024 - 24 not given, set to 0
%       MVA limit of branch 7001 - 1 not given, set to 0
%       MVA limit of branch 7130 - 130 not given, set to 0
%       MVA limit of branch 7011 - 11 not given, set to 0
%       MVA limit of branch 7023 - 23 not given, set to 0
%       MVA limit of branch 7049 - 49 not given, set to 0
%       MVA limit of branch 7139 - 139 not given, set to 0
%       MVA limit of branch 7012 - 12 not given, set to 0
%       MVA limit of branch 7017 - 17 not given, set to 0
%       MVA limit of branch 7039 - 39 not given, set to 0
%       MVA limit of branch 7057 - 57 not given, set to 0
%       MVA limit of branch 7044 - 44 not given, set to 0
%       MVA limit of branch 7055 - 55 not given, set to 0
%       MVA limit of branch 7071 - 71 not given, set to 0
%
%   See CASEFORMAT for details on the MATPOWER case file format.

%% MATPOWER Case Format : Version 2
mpc.version = '2';

%%-----  Power Flow Data  -----%%
%% system MVA base
mpc.baseMVA = 100;

%% bus data
%	bus_i	type	Pd	Qd	Gs	Bs	area	Vm	Va	baseKV	zone	Vmax	Vmin
mpc.bus = [
	1	1	90	49	0	0	1	1.0284	5.95	115	1	1.06	0.94;
	2	1	56	15	0	0	1	1.0354	7.74	115	1	1.06	0.94;
	3	1	20	0	0	0	1	0.9971	6.64	230	1	1.06	0.94;
	4	1	0	0	0	0	1	1.0308	4.71	345	1	1.06	0.94;
	5	1	353	130	0	0	1	1.0191	4.68	115	1	1.06	0.94;
	6	1	120	41	0	0	1	1.0312	6.99	115	1	1.06	0.94;
	7	1	0	0	0	0	1	0.9934	6.19	230	1	1.06	0.94;
	8	2	63	14	0	0	1	1.0153	2.4	115	1	1.06	0.94;
	9	1	96	43	0	0	1	1.0034	2.85	115	1	1.06	0.94;
	10	2	153	33	0	0	1	1.0205	1.35	230	1	1.06	0.94;
	11	1	83	21	0	0	1	1.0057	2.46	115	1	1.06	0.94;
	12	1	0	0	0	0	1	0.9974	5.21	230	1	1.06	0.94;
	13	1	58	10	0	0	1	0.9977	-0.55	115	1	1.06	0.94;
	14	1	160	60	0	0	1	0.9991	-4.81	115	1	1.06	0.94;
	15	1	126.7	23	0	0	1	1.0343	-8.59	115	1	1.06	0.94;
	16	1	0	0	0	0	1	1.0315	-2.65	345	1	1.06	0.94;
	17	1	561	220	0	0	1	1.0649	-13.1	115	1	1.06	0.94;
	19	1	0	0	0	0	1	0.982	1.08	230	1	1.06	0.94;
	20	2	605	120	0	0	1	1.001	-2.46	115	1	1.06	0.94;
	21	1	77	1	0	0	1	0.9752	1.62	230	1	1.06	0.94;
	22	1	81	23	0	0	1	0.9963	-1.97	115	1	1.06	0.94;
	23	1	21	7	0	0	1	1.0501	3.94	115	1	1.06	0.94;
	24	1	0	0	0	0	1	1.0057	6.02	230	1	1.06	0.94;
	25	1	45	12	0	0	1	1.0234	1.44	115	1	1.06	0.94;
	26	1	28	9	0	0	1	0.9986	-1.73	115	1	1.06	0.94;
	27	1	69	13	0	0	1	0.975	-4.9	115	1	1.06	0.94;
	33	1	55	6	0	0	1	1.0244	-12.02	115	1	1.06	0.94;
	34	1	0	0	0	0	1	1.0414	-7.94	345	1	1.06	0.94;
	35	1	0	0	0	0	1	0.9757	-25.72	115	1	1.06	0.94;
	36	1	0	0	0	0	1	1.0011	-22.59	230	1	1.06	0.94;
	37	1	85	32	0	0	1	1.0201	-11.23	115	1	1.06	0.94;
	38	1	155	18	0	0	1	1.0202	-12.56	115	1	1.06	0.94;
	39	1	0	0	0	0	1	1.0535	-5.81	345	1	1.06	0.94;
	40	1	46	-21	0	0	1	1.0216	-12.78	115	1	1.06	0.94;
	41	1	86	0	0	0	1	1.0292	-10.45	115	1	1.06	0.94;
	42	1	0	0	0	0	1	1.0448	-7.44	345	1	1.06	0.94;
	43	1	39	9	0	0	1	1.0006	-16.79	115	1	1.06	0.94;
	44	1	195	29	0	0	1	1.0086	-17.47	115	1	1.06	0.94;
	45	1	0	0	0	0	1	1.0215	-14.74	230	1	1.06	0.94;
	46	1	0	0	0	0	1	1.0344	-11.75	345	1	1.06	0.94;
	47	1	58	11.8	0	0	1	0.9777	-23.17	115	1	1.06	0.94;
	48	1	41	19	0	0	1	1.0019	-16.09	115	1	1.06	0.94;
	49	1	92	26	0	0	1	1.0475	-2.95	115	1	1.06	0.94;
	51	1	-5	5	0	0	1	1.0253	-8.15	115	1	1.06	0.94;
	52	1	61	28	0	0	1	0.9979	-11.86	115	1	1.06	0.94;
	53	1	69	3	0	0	1	0.9959	-17.6	115	1	1.06	0.94;
	54	1	10	1	0	0	1	1.005	-16.25	115	1	1.06	0.94;
	55	1	22	10	0	0	1	1.015	-12.21	115	1	1.06	0.94;
	57	1	98	20	0	0	1	1.0335	-8	115	1	1.06	0.94;
	58	1	14	1	0	0	1	0.9918	-5.99	115	1	1.06	0.94;
	59	1	218	106	0	0	1	0.9789	-5.29	115	1	1.06	0.94;
	60	1	0	0	0	0	1	1.0246	-9.56	230	1	1.06	0.94;
	61	1	227	110	0	0	1	0.9906	-3.47	115	1	1.06	0.94;
	62	1	0	0	0	0	1	1.016	-1.1	230	1	1.06	0.94;
	63	2	70	30	0	0	1	0.9583	-17.62	115	1	1.06	0.94;
	64	1	0	0	0	0	1	0.948	-12.97	230	1	1.06	0.94;
	69	1	0	0	0	0	1	0.963	-25.66	115	1	1.06	0.94;
	70	1	56	20	0	0	1	0.9513	-35.16	115	1	1.06	0.94;
	71	1	116	38	0	0	1	0.9793	-29.88	115	1	1.06	0.94;
	72	1	57	19	0	0	1	0.9696	-27.48	115	1	1.06	0.94;
	73	1	224	71	0	0	1	0.9775	-25.77	115	1	1.06	0.94;
	74	1	0	0	0	0	1	0.9964	-22	230	1	1.06	0.94;
	76	2	208	107	0	0	1	0.9632	-26.54	115	1	1.06	0.94;
	77	1	74	28	0	0	1	0.9837	-24.94	115	1	1.06	0.94;
	78	1	0	0	0	0	1	0.99	-24.05	115	1	1.06	0.94;
	79	1	48	14	0	0	1	0.982	-24.97	115	1	1.06	0.94;
	80	1	28	7	0	0	1	0.9872	-24.97	115	1	1.06	0.94;
	81	1	0	0	0	0	1	1.034	-18.89	345	1	1.06	0.94;
	84	2	37	13	0	0	1	1.025	-17.16	115	1	1.06	0.94;
	85	1	0	0	0	0	1	0.9872	-17.68	230	1	1.06	0.94;
	86	1	0	0	0	0	1	0.9909	-14.19	230	1	1.06	0.94;
	87	1	0	0	0	0	1	0.9921	-7.77	230	1	1.06	0.94;
	88	1	0	0	0	0	1	1.0151	-20.96	230	1	1.06	0.94;
	89	1	44.2	0	0	0	1	1.0317	-11.13	115	1	1.06	0.94;
	90	1	66	0	0	0	1	1.0272	-11.23	115	1	1.06	0.94;
	91	2	17.4	0	0	0	1	1.052	-9.4	115	1	1.06	0.94;
	92	2	15.8	0	0	0	1	1.052	-6.2	115	1	1.06	0.94;
	94	1	60.3	0	0	0	1	0.993	-9.42	115	1	1.06	0.94;
	97	1	39.9	0	0	0	1	1.0183	-13.24	115	1	1.06	0.94;
	98	2	66.7	0	0	0	1	1	-14.6	115	1	1.06	0.94;
	99	1	83.5	0	0	0	1	0.9894	-20.27	115	1	1.06	0.94;
	100	1	0	0	0	0	1	1.006	-14.45	115	1	1.06	0.94;
	102	1	77.8	0	0	0	1	1.0008	-15.23	115	1	1.06	0.94;
	103	1	32	0	0	0	1	1.0288	-12.06	115	1	1.06	0.94;
	104	1	8.6	0	0	0	1	0.9958	-17.33	115	1	1.06	0.94;
	105	1	49.6	0	0	0	1	1.0223	-12.94	115	1	1.06	0.94;
	107	1	4.6	0	0	0	1	1.0095	-16.03	115	1	1.06	0.94;
	108	2	112.1	0	0	0	1	0.99	-20.26	115	1	1.06	0.94;
	109	1	30.7	0	0	0	1	0.9749	-26.06	115	1	1.06	0.94;
	110	1	63	0	0	0	1	0.973	-24.72	115	1	1.06	0.94;
	112	1	19.6	0	0	0	1	0.9725	-28.69	115	1	1.06	0.94;
	113	1	26.2	0	0	0	1	0.97	-25.38	115	1	1.06	0.94;
	114	1	18.2	0	0	0	1	0.9747	-28.59	115	1	1.06	0.94;
	115	1	0	0	0	0	1	0.9603	-13.57	115	2	1.06	0.94;
	116	1	0	0	0	0	1	1.0249	-12.69	115	2	1.06	0.94;
	117	1	0	0	0	325	1	0.9348	-4.72	115	2	1.06	0.94;
	118	1	14.1	650	0	0	1	0.9298	-4.12	115	2	1.06	0.94;
	119	2	0	0	0	0	1	1.0435	5.17	115	2	1.06	0.94;
	120	1	777	215	0	55	1	0.9584	-8.77	115	2	1.06	0.94;
	121	1	535	55	0	0	1	0.9871	-12.64	115	2	1.06	0.94;
	122	1	229.1	11.8	0	0	1	0.9728	-14.36	115	2	1.06	0.94;
	123	1	78	1.4	0	0	1	1.0006	-17.64	115	2	1.06	0.94;
	124	2	276.4	59.3	0	0	1	1.0233	-13.49	115	2	1.06	0.94;
	125	2	514.8	82.7	0	0	1	1.0103	-18.43	115	2	1.06	0.94;
	126	1	57.9	5.1	0	0	1	0.9978	-12.86	115	2	1.06	0.94;
	127	1	380.8	37	0	0	1	1.0001	-10.52	230	2	1.06	0.94;
	128	1	0	0	0	0	1	1.0024	-4.78	230	2	1.06	0.94;
	129	1	0	0	0	0	1	1.0028	-4.4	230	2	1.06	0.94;
	130	1	0	0	0	0	1	1.0191	5.56	230	2	1.06	0.94;
	131	1	0	0	0	0	1	0.9861	6.06	230	2	1.06	0.94;
	132	1	0	0	0	0	1	1.0045	3.04	230	2	1.06	0.94;
	133	1	0	0	0	0	1	1.002	-5.46	230	2	1.06	0.94;
	134	1	0	0	0	0	1	1.022	-8.04	230	2	1.06	0.94;
	135	1	169.2	41.6	0	0	1	1.0193	-6.76	230	2	1.06	0.94;
	136	1	55.2	18.2	0	0	1	1.0476	1.54	230	2	1.06	0.94;
	137	1	273.6	99.8	0	0	1	1.0471	-1.45	230	2	1.06	0.94;
	138	2	1019.2	135.2	0	0	1	1.055	-6.35	230	2	1.06	0.94;
	139	1	595	83.3	0	0	1	1.0117	-3.57	115	2	1.06	0.94;
	140	1	387.7	114.7	0	0	1	1.043	-3.44	230	2	1.06	0.94;
	141	2	145	58	0	0	1	1.051	0.05	230	2	1.06	0.94;
	142	1	56.5	24.5	0	0	1	1.0155	-2.77	230	2	1.06	0.94;
	143	2	89.5	35.5	0	0	1	1.0435	4.03	230	2	1.06	0.94;
	144	1	0	0	0	0	1	1.016	-0.7	230	2	1.06	0.94;
	145	1	24	14	0	0	1	1.0081	-0.16	230	2	1.06	0.94;
	146	2	0	0	0	0	1	1.0528	4.32	230	2	1.06	0.94;
	147	2	0	0	0	0	1	1.0528	8.36	230	2	1.06	0.94;
	148	1	63	25	0	0	1	1.0577	0.28	230	2	1.06	0.94;
	149	2	0	0	0	0	1	1.0735	5.23	230	2	1.06	0.94;
	150	1	0	0	0	0	1	0.9869	6.34	230	2	1.06	0.94;
	151	1	0	0	0	0	1	1.0048	4.13	230	2	1.06	0.94;
	152	2	17	9	0	0	1	1.0535	9.24	230	2	1.06	0.94;
	153	2	0	0	0	0	1	1.0435	10.46	230	2	1.06	0.94;
	154	1	70	5	0	34.5	1	0.9663	-1.8	115	2	1.06	0.94;
	155	1	200	50	0	0	1	1.0177	6.75	230	2	1.06	0.94;
	156	2	75	50	0	0	1	0.963	5.15	115	2	1.06	0.94;
	157	1	123.5	-24.3	0	0	1	0.9845	-11.93	230	2	1.06	0.94;
	158	1	0	0	0	0	1	0.9987	-11.4	230	2	1.06	0.94;
	159	1	33	16.5	0	0	1	0.9867	-9.82	230	2	1.06	0.94;
	160	1	0	0	0	0	1	0.9998	-12.55	230	2	1.06	0.94;
	161	1	35	15	0	0	1	1.036	8.85	230	2	1.06	0.94;
	162	1	85	24	0	0	1	0.9918	18.5	230	2	1.06	0.94;
	163	1	0	0.4	0	0	1	1.041	2.91	230	2	1.06	0.94;
	164	1	0	0	0	-212	1	0.9839	9.66	230	2	1.06	0.94;
	165	1	0	0	0	0	1	1.0002	26.31	230	2	1.06	0.94;
	166	1	0	0	0	-103	1	0.9973	30.22	230	2	1.06	0.94;
	167	1	299.9	95.7	0	0	1	0.9715	-6.91	230	2	1.06	0.94;
	168	1	0	0	0	0	1	1.0024	-4.8	230	2	1.06	0.94;
	169	1	0	0	0	0	1	0.9879	-6.68	230	2	1.06	0.94;
	170	2	481.8	205	0	0	1	0.929	0.09	115	2	1.06	0.94;
	171	2	763.6	291.1	0	0	1	0.9829	-9.94	115	2	1.06	0.94;
	172	1	26.5	0	0	0	1	1.0244	-6.22	115	2	1.06	0.94;
	173	1	163.5	43	0	53	1	0.9837	-12.75	115	2	1.06	0.94;
	174	1	0	0	0	0	1	1.0622	-2.69	115	2	1.06	0.94;
	175	1	176	83	0	0	1	0.973	-7.21	115	2	1.06	0.94;
	176	2	5	4	0	0	1	1.0522	4.67	115	2	1.06	0.94;
	177	2	28	12	0	0	1	1.0077	0.62	115	2	1.06	0.94;
	178	1	427.4	173.6	0	0	1	0.9397	-6.56	115	2	1.06	0.94;
	179	1	74	29	0	45	1	0.9699	-9.37	115	2	1.06	0.94;
	180	1	69.5	49.3	0	0	1	0.9793	-3.09	115	2	1.06	0.94;
	181	1	73.4	0	0	0	1	1.0518	-1.33	230	2	1.06	0.94;
	182	1	240.7	89	0	0	1	1.0447	-4.19	230	2	1.06	0.94;
	183	1	40	4	0	0	1	0.9717	7.12	115	2	1.06	0.94;
	184	1	136.8	16.6	0	0	1	1.0386	-6.85	230	2	1.06	0.94;
	185	2	0	0	0	0	1	1.0522	-4.33	230	2	1.06	0.94;
	186	2	59.8	24.3	0	0	1	1.065	2.17	230	2	1.06	0.94;
	187	2	59.8	24.3	0	0	1	1.065	1.4	230	2	1.06	0.94;
	188	1	182.6	43.6	0	0	1	1.0533	-0.72	230	2	1.06	0.94;
	189	1	7	2	0	0	1	0.9975	-25.84	66	3	1.06	0.94;
	190	2	0	0	0	-150	1	1.0551	-20.62	345	3	1.06	0.94;
	191	2	489	53	0	0	1	1.0435	12.25	230	3	1.06	0.94;
	192	1	800	72	0	0	1	0.9374	-11.18	230	3	1.06	0.94;
	193	1	0	0	0	0	1	0.9897	-26.09	66	3	1.06	0.94;
	194	1	0	0	0	0	1	1.0489	-19.21	345	3	1.06	0.94;
	195	1	0	0	0	0	1	1.0357	-20.79	345	3	1.06	0.94;
	196	1	10	3	0	0	1	0.9695	-25.32	115	3	1.06	0.94;
	197	1	43	14	0	0	1	0.9907	-23.72	115	3	1.06	0.94;
	198	2	64	21	0	0	1	1.015	-20.58	115	3	1.06	0.94;
	199	1	35	12	0	0	1	0.9528	-26.05	115	3	1.06	0.94;
	200	1	27	12	0	0	1	0.955	-25.93	115	3	1.06	0.94;
	201	1	41	14	0	0	1	0.9692	-27.49	66	1	1.06	0.94;
	202	1	38	13	0	0	1	0.9908	-25.33	66	3	1.06	0.94;
	203	1	42	14	0	0	1	1.0033	-22.35	115	3	1.06	0.94;
	204	1	72	24	0	0	1	0.9718	-25.7	66	3	1.06	0.94;
	205	1	0	-5	0	0	1	0.9838	-26.07	66	3	1.06	0.94;
	206	1	12	2	0	0	1	0.9992	-27.41	66	3	1.06	0.94;
	207	1	-21	-14.2	0	0	1	1.0137	-27.44	66	1	1.06	0.94;
	208	1	7	2	0	0	1	0.9929	-26.28	66	3	1.06	0.94;
	209	1	38	13	0	0	1	0.9999	-25.66	66	3	1.06	0.94;
	210	1	0	0	0	0	1	0.9788	-24.22	115	3	1.06	0.94;
	211	1	96	7	0	0	1	1.0017	-23.31	115	3	1.06	0.94;
	212	1	0	0	0	0	1	1.0132	-22.51	138	3	1.06	0.94;
	213	2	0	0	0	0	1	1.01	-11.67	16.5	3	1.06	0.94;
	214	1	22	16	0	0	1	0.9919	-17.53	138	3	1.06	0.94;
	215	1	47	26	0	0	1	0.9866	-20.23	138	3	1.06	0.94;
	216	1	176	105	0	0	1	0.9751	-22.53	138	3	1.06	0.94;
	217	1	100	75	0	0	1	1.0215	-22.2	138	3	1.06	0.94;
	218	1	131	96	0	0	1	1.0075	-22.63	138	3	1.06	0.94;
	219	1	0	0	0	0	1	1.0554	-21.15	345	3	1.06	0.94;
	220	2	285	100	0	0	1	1.008	-21.73	138	3	1.06	0.94;
	221	2	171	70	0	0	1	1	-22.49	138	3	1.06	0.94;
	222	2	328	188	0	0	1	1.05	-23.17	20	3	1.06	0.94;
	223	1	428	232	0	0	1	0.9965	-22.7	138	3	1.06	0.94;
	224	1	173	99	0	0	1	1.0002	-21.55	230	3	1.06	0.94;
	225	1	410	40	0	0	1	0.9453	-11.34	230	3	1.06	0.94;
	226	1	0	0	0	0	1	1.018	-21.61	230	3	1.06	0.94;
	227	2	538	369	0	0	1	1	-27.22	27	3	1.06	0.94;
	228	1	223	148	0	0	1	1.0423	-20.94	138	3	1.06	0.94;
	229	1	96	46	0	0	1	1.0496	-19.94	138	3	1.06	0.94;
	230	2	0	0	0	0	1	1.04	-13.82	20	3	1.06	0.94;
	231	1	159	107	0	-300	1	1.0535	-21.22	345	3	1.06	0.94;
	232	1	448	143	0	0	1	1.0414	-23.19	138	3	1.06	0.94;
	233	2	404	212	0	0	1	1	-25.9	66	3	1.06	0.94;
	234	1	572	244	0	0	1	1.0387	-20.89	138	3	1.06	0.94;
	235	1	269	157	0	0	1	1.0095	-21.03	138	3	1.06	0.94;
	236	2	0	0	0	0	1	1.0165	-15.4	20	3	1.06	0.94;
	237	1	0	0	0	0	1	1.0558	-21.1	345	3	1.06	0.94;
	238	2	255	149	0	-150	1	1.01	-20.94	138	3	1.06	0.94;
	239	2	0	0	0	0	1	1	-15.86	138	3	1.06	0.94;
	240	1	0	0	0	-140	1	1.0237	-20.14	230	3	1.06	0.94;
	241	2	0	0	0	0	1	1.05	-16.5	20	3	1.06	0.94;
	242	2	0	0	0	0	1	0.993	-17.53	138	3	1.06	0.94;
	243	2	8	3	0	0	1	1.01	-19.27	66	3	1.06	0.94;
	244	1	0	0	0	0	1	0.9921	-20.21	66	3	1.06	0.94;
	245	1	61	30	0	0	1	0.9711	-20.9	66	3	1.06	0.94;
	246	1	77	33	0	0	1	0.9651	-21.74	66	3	1.06	0.94;
	247	1	61	30	0	0	1	0.9688	-21.67	66	3	1.06	0.94;
	248	1	29	14	0	45.6	1	0.976	-25.23	66	3	1.06	0.94;
	249	1	29	14	0	0	1	0.9752	-25.65	66	3	1.06	0.94;
	250	1	-23	-17	0	0	1	1.0196	-23.8	66	3	1.06	0.94;
	281	1	-33.1	-29.4	0	0	1	1.0251	-20.06	230	3	1.06	0.94;
	319	1	115.8	-24	0	0	1	1.0152	1.48	230	1	1.06	0.94;
	320	1	2.4	-12.6	0	0	1	1.0146	-2.23	115	1	1.06	0.94;
	322	1	2.4	-3.9	0	0	1	1.0005	-17.61	115	1	1.06	0.94;
	323	1	-14.9	26.5	0	0	1	0.981	-13.69	230	1	1.06	0.94;
	324	1	24.7	-1.2	0	0	1	0.975	-23.42	115	1	1.06	0.94;
	526	1	145.3	-34.9	0	0	1	0.9429	-34.31	115	1	1.06	0.94;
	528	1	28.1	-20.5	0	0	1	0.9723	-37.58	115	1	1.06	0.94;
	531	1	14	2.5	0	0	1	0.9604	-29.1	115	1	1.06	0.94;
	552	1	-11.1	-1.4	0	0	1	1.0009	-23.36	115	1	1.06	0.94;
	562	1	50.5	17.4	0	0	1	0.9777	-28	230	1	1.06	0.94;
	609	1	29.6	0.6	0	0	1	0.9583	-28.79	115	1	1.06	0.94;
	664	1	-113.7	76.7	0	0	1	1.0309	-17	345	3	1.06	0.94;
	1190	1	100.31	29.17	0	0	1	1.0128	3.9	86	2	1.06	0.94;
	1200	1	-100	34.17	0	0	1	1.0244	-7.52	86	2	1.06	0.94;
	1201	1	0	0	0	0	1	1.0122	-15.18	115	2	1.06	0.94;
	2040	1	0	0	0	0	1	0.9653	-14.94	115	3	1.06	0.94;
	7001	2	0	0	0	0	1	1.0507	10.79	13.8	1	1.06	0.94;
	7002	2	0	0	0	0	1	1.0507	12.48	13.8	1	1.06	0.94;
	7003	2	0	0	0	0	1	1.0323	13.76	13.8	1	1.06	0.94;
	7011	2	0	0	0	0	1	1.0145	4.99	13.8	1	1.06	0.94;
	7012	2	0	0	0	0	1	1.0507	11.57	13.8	1	1.06	0.94;
	7017	2	0	0	0	0	1	1.0507	-10.47	13.8	1	1.06	0.94;
	7023	2	0	0	0	0	1	1.0507	6.15	13.8	1	1.06	0.94;
	7024	2	0	0	0	0	1	1.029	12.6	13.8	1	1.06	0.94;
	7039	2	0	0	0	0	1	1.05	2.11	20	1	1.06	0.94;
	7044	2	0	0	0	0	1	1.0145	-13.92	13.8	1	1.06	0.94;
	7049	3	0	0	0	0	1	1.0507	0	13.8	1	1.06	0.94;
	7055	2	0	0	0	0	1	0.9967	-7.5	13.8	1	1.06	0.94;
	7057	2	0	0	0	0	1	1.0212	-3.44	13.8	1	1.06	0.94;
	7061	2	0	0	0	0	1	1.0145	1.97	13.8	1	1.06	0.94;
	7062	2	0	0	0	0	1	1.0017	5.8	13.8	1	1.06	0.94;
	7071	2	0	0	0	0	1	0.9893	-25.35	13.8	1	1.06	0.94;
	7130	2	0	0	0	0	1	1.0507	19.02	13.8	2	1.06	0.94;
	7139	2	0	0	0	0	1	1.0507	2.75	13.8	2	1.06	0.94;
	7166	2	0	0	0	0	1	1.0145	35.05	13.8	2	1.06	0.94;
	9001	1	0	0	0	0	1	1.0117	-11.25	115	9	1.06	0.94;
	9002	2	4.2	0	0	0	1	0.9945	-18.86	6.6	9	1.06	0.94;
	9003	1	2.71	0.94	0.14	2.4	1	0.9833	-19.68	6.6	9	1.06	0.94;
	9004	1	0.86	0.28	0	0	1	0.9768	-19.82	6.6	9	1.06	0.94;
	9005	1	0	0	0	0	1	1.0117	-11.32	115	9	1.06	0.94;
	9006	1	0	0	0	0	1	1.0029	-17.42	6.6	9	1.06	0.94;
	9007	1	0	0	0	0	1	0.9913	-18.69	6.6	9	1.06	0.94;
	9012	1	0	0	0	0	1	1.0023	-17.27	6.6	9	1.06	0.94;
	9021	1	4.75	1.56	0	0	1	0.9887	-19.09	6.6	9	1.06	0.94;
	9022	1	1.53	0.53	0.08	0	1	0.9648	-21.67	0.6	9	1.06	0.94;
	9023	1	0	0	0	0	1	0.9747	-19.41	6.6	9	1.06	0.94;
	9024	1	1.35	0.47	0.07	0	1	0.9706	-21.43	0.6	9	1.06	0.94;
	9025	1	0.45	0.16	0.02	0	1	0.9649	-20.48	0.6	9	1.06	0.94;
	9026	1	0.45	0.16	0.02	0	1	0.9657	-20.39	0.6	9	1.06	0.94;
	9031	1	1.84	0.64	0.1	0	1	0.9318	-25.03	0.6	9	1.06	0.94;
	9032	1	1.39	0.48	0.07	0	1	0.9441	-23.84	0.6	9	1.06	0.94;
	9033	1	1.89	0.65	0.1	0	1	0.9286	-25.33	0.6	9	1.06	0.94;
	9034	1	1.55	0.54	0.08	1.72	1	0.9973	-21.1	0.6	9	1.06	0.94;
	9035	1	1.66	0.58	0.09	0	1	0.9506	-23.19	0.6	9	1.06	0.94;
	9036	1	3.03	1	0	0	1	0.9598	-22.67	2.3	9	1.06	0.94;
	9037	1	1.86	0.64	0.1	0	1	0.957	-22.58	0.6	9	1.06	0.94;
	9038	1	2.58	0.89	0.14	0	1	0.9391	-24.41	0.6	9	1.06	0.94;
	9041	1	1.01	0.35	0.05	0	1	0.9636	-21.33	0.6	9	1.06	0.94;
	9042	1	0.81	0.28	0.04	0	1	0.9501	-22.5	0.6	9	1.06	0.94;
	9043	1	1.6	0.52	0	0	1	0.9646	-21.42	2.3	9	1.06	0.94;
	9044	1	0	0	0	0	1	0.979	-19.78	6.6	9	1.06	0.94;
	9051	2	35.81	0	0	0	1	1	-19.4	13.8	9	1.06	0.94;
	9052	1	30	23	0	0	1	0.9786	-17.25	13.8	9	1.06	0.94;
	9053	2	26.48	0	0	0	1	1	-17.68	13.8	9	1.06	0.94;
	9054	2	0	0	0	0	1	1	-6.83	13.8	9	1.06	0.94;
	9055	2	0	0	0	0	1	1	-7.54	13.8	9	1.06	0.94;
	9071	1	1.02	0.35	0.05	0	1	0.9752	-20.48	0.6	9	1.06	0.94;
	9072	1	1.02	0.35	0.05	0	1	0.9803	-19.92	0.6	9	1.06	0.94;
	9121	1	3.8	1.25	0	0	1	0.9799	-19.3	6.6	9	1.06	0.94;
	9533	1	1.19	0.41	0.1	0	1	1.0402	-18.24	2.3	9	1.06	0.94;
];

%% generator data
%	bus	Pg	Qg	Qmax	Qmin	Vg	mBase	status	Pmax	Pmin	Pc1	Pc2	Qc1min	Qc1max	Qc2min	Qc2max	ramp_agc	ramp_10	ramp_30	ramp_q	apf
mpc.gen = [
	8	0	0	10	-10	1.0153	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	10	0	0	20	-20	1.0205	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	20	0	0	20	-20	1.001	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	63	0	0	25	-25	0.9583	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	76	0	0	35	12	0.9632	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	84	375	0	240	-240	1.025	100	1	475	0	0	0	0	0	0	0	0	0	0	0	0;
	91	155	0	96	-11	1.052	100	1	255	0	0	0	0	0	0	0	0	0	0	0	0;
	92	290	0	153	-153	1.052	100	1	390	0	0	0	0	0	0	0	0	0	0	0	0;
	98	68	0	56	-30	1	100	1	168	0	0	0	0	0	0	0	0	0	0	0	0;
	108	117	0	77	-24	0.99	100	1	217	0	0	0	0	0	0	0	0	0	0	0	0;
	119	1930	0	1500	-500	1.0435	100	1	2030	0	0	0	0	0	0	0	0	0	0	0	0;
	124	240	0	120	-60	1.0233	100	1	340	0	0	0	0	0	0	0	0	0	0	0	0;
	125	0	0	200	-25	1.0103	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	138	0	0	350	-125	1.055	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	141	281	0	75	-50	1.051	100	1	381	0	0	0	0	0	0	0	0	0	0	0	0;
	143	696	0	300	-100	1.0435	100	1	796	0	0	0	0	0	0	0	0	0	0	0	0;
	146	84	0	35	-15	1.0528	100	1	184	0	0	0	0	0	0	0	0	0	0	0	0;
	147	217	0	100	-50	1.0528	100	1	317	0	0	0	0	0	0	0	0	0	0	0	0;
	149	103	0	50	-25	1.0735	100	1	203	0	0	0	0	0	0	0	0	0	0	0	0;
	152	372	0	175	-50	1.0535	100	1	472	0	0	0	0	0	0	0	0	0	0	0	0;
	153	216	0	90	-50	1.0435	100	1	316	0	0	0	0	0	0	0	0	0	0	0	0;
	156	0	0	15	-10	0.963	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	170	205	0	90	-40	0.929	100	1	305	0	0	0	0	0	0	0	0	0	0	0	0;
	171	0	0	150	-50	0.9829	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	176	228	0	90	-45	1.0522	100	1	328	0	0	0	0	0	0	0	0	0	0	0	0;
	177	84	0	35	-15	1.0077	100	1	184	0	0	0	0	0	0	0	0	0	0	0	0;
	185	200	0	80	-50	1.0522	100	1	300	0	0	0	0	0	0	0	0	0	0	0	0;
	186	1200	0	400	-100	1.065	100	1	1300	0	0	0	0	0	0	0	0	0	0	0	0;
	187	1200	0	400	-100	1.065	100	1	1300	0	0	0	0	0	0	0	0	0	0	0	0;
	190	475	0	300	-300	1.0551	100	1	575	0	0	0	0	0	0	0	0	0	0	0	0;
	191	1973	0	1000	-1000	1.0435	100	1	2073	0	0	0	0	0	0	0	0	0	0	0	0;
	198	424	0	260	-260	1.015	100	1	524	0	0	0	0	0	0	0	0	0	0	0	0;
	213	272	0	150	-150	1.01	100	1	372	0	0	0	0	0	0	0	0	0	0	0	0;
	220	100	0	60	-60	1.008	100	1	200	0	0	0	0	0	0	0	0	0	0	0	0;
	221	450	0	320	-320	1	100	1	550	0	0	0	0	0	0	0	0	0	0	0	0;
	222	250	0	300	-300	1.05	100	1	350	0	0	0	0	0	0	0	0	0	0	0	0;
	227	303	0	300	-300	1	100	1	403	0	0	0	0	0	0	0	0	0	0	0	0;
	230	345	0	250	-250	1.04	100	1	445	0	0	0	0	0	0	0	0	0	0	0	0;
	233	300	0	500	-500	1	100	1	400	0	0	0	0	0	0	0	0	0	0	0	0;
	236	600	0	300	-300	1.0165	100	1	700	0	0	0	0	0	0	0	0	0	0	0	0;
	238	250	0	200	-200	1.01	100	1	350	0	0	0	0	0	0	0	0	0	0	0	0;
	239	550	0	400	-400	1	100	1	650	0	0	0	0	0	0	0	0	0	0	0	0;
	241	575.43	0	600	-600	1.05	100	1	675.43	0	0	0	0	0	0	0	0	0	0	0	0;
	242	170	0	100	40	0.993	100	1	270	0	0	0	0	0	0	0	0	0	0	0	0;
	243	84	0	80	40	1.01	100	1	184	0	0	0	0	0	0	0	0	0	0	0	0;
	7001	467	0	210	-210	1.0507	100	1	567	0	0	0	0	0	0	0	0	0	0	0	0;
	7002	623	0	280	-280	1.0507	100	1	723	0	0	0	0	0	0	0	0	0	0	0	0;
	7003	1210	0	420	-420	1.0323	100	1	1310	0	0	0	0	0	0	0	0	0	0	0	0;
	7011	234	0	100	-100	1.0145	100	1	334	0	0	0	0	0	0	0	0	0	0	0	0;
	7012	372	0	224	-224	1.0507	100	1	472	0	0	0	0	0	0	0	0	0	0	0	0;
	7017	330	0	350	0	1.0507	100	1	430	0	0	0	0	0	0	0	0	0	0	0	0;
	7023	185	0	120	0	1.0507	100	1	285	0	0	0	0	0	0	0	0	0	0	0	0;
	7024	410	0	224	-224	1.029	100	1	510	0	0	0	0	0	0	0	0	0	0	0	0;
	7039	500	0	200	-200	1.05	100	1	600	0	0	0	0	0	0	0	0	0	0	0	0;
	7044	37	0	42	0	1.0145	100	1	137	0	0	0	0	0	0	0	0	0	0	0	0;
	7049	0	0	10	0	1.0507	100	1	2399.005	0	0	0	0	0	0	0	0	0	0	0	0;
	7055	45	0	25	0	0.9967	100	1	145	0	0	0	0	0	0	0	0	0	0	0	0;
	7057	165	0	90	-90	1.0212	100	1	265	0	0	0	0	0	0	0	0	0	0	0	0;
	7061	400	0	150	-150	1.0145	100	1	500	0	0	0	0	0	0	0	0	0	0	0	0;
	7062	400	0	150	0	1.0017	100	1	500	0	0	0	0	0	0	0	0	0	0	0	0;
	7071	116	0	87	0	0.9893	100	1	216	0	0	0	0	0	0	0	0	0	0	0	0;
	7130	1292	0	600	-100	1.0507	100	1	1392	0	0	0	0	0	0	0	0	0	0	0	0;
	7139	700	0	325	-125	1.0507	100	1	800	0	0	0	0	0	0	0	0	0	0	0	0;
	7166	553	0	300	-200	1.0145	100	1	653	0	0	0	0	0	0	0	0	0	0	0	0;
	9002	0	0	2	-2	0.9945	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	9051	0	0	17.35	-17.35	1	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	9053	0	0	12.83	-12.8	1	100	1	100	0	0	0	0	0	0	0	0	0	0	0	0;
	9054	50	0	38	-38	1	100	1	150	0	0	0	0	0	0	0	0	0	0	0	0;
	9055	8	0	6	-6	1	100	1	108	0	0	0	0	0	0	0	0	0	0	0	0;
];

%% branch data
%	fbus	tbus	r	x	b	rateA	rateB	rateC	ratio	angle	status	angmin	angmax
mpc.branch = [
	37	9001	6e-05	0.00046	0	0	0	75	1.0082	0	1	-360	360;
	9001	9005	0.0008	0.00348	0	0	0	0	0	0	1	-360	360;
	9001	9006	0.02439	0.43682	0	0	0	0	0.9668	0	1	-360	360;
	9001	9012	0.03624	0.64898	0	0	0	0	0.9796	0	1	-360	360;
	9005	9051	0.01578	0.37486	0	0	0	0	1.0435	0	1	-360	360;
	9005	9052	0.01578	0.37486	0	0	0	0	0.9391	0	1	-360	360;
	9005	9053	0.01602	0.38046	0	0	0	0	1.0435	0	1	-360	360;
	9005	9054	0	0.152	0	0	0	0	1.0435	0	1	-360	360;
	9005	9055	0	0.8	0	0	0	0	1.0435	0	1	-360	360;
	9006	9007	0.05558	0.24666	0	0	0	0	0	0	1	-360	360;
	9006	9003	0.11118	0.49332	0	0	0	0	0	0	1	-360	360;
	9006	9003	0.11118	0.49332	0	0	0	0	0	0	1	-360	360;
	9012	9002	0.07622	0.43286	0	0	0	0	0	0	1	-360	360;
	9012	9002	0.07622	0.43286	0	0	0	0	0	0	1	-360	360;
	9002	9021	0.0537	0.07026	0	0	0	0	0	0	1	-360	360;
	9021	9023	1.1068	0.95278	0	0	0	0	0	0	1	-360	360;
	9021	9022	0.44364	2.8152	0	0	0	0	1	0	1	-360	360;
	9002	9024	0.50748	3.2202	0	0	0	0	1	0	1	-360	360;
	9023	9025	0.66688	3.944	0	0	0	0	1	0	1	-360	360;
	9023	9026	0.6113	3.6152	0	0	0	0	1	0	1	-360	360;
	9007	9071	0.4412	2.9668	0	0	0	0	1	0	1	-360	360;
	9007	9072	0.30792	2.057	0	0	0	0	1	0	1	-360	360;
	9007	9003	0.0558	0.24666	0	0	0	0	0	0	1	-360	360;
	9003	9031	0.73633	4.6724	0	0	0	0	1	0	1	-360	360;
	9003	9032	0.76978	4.8846	0	0	0	0	1	0	1	-360	360;
	9003	9033	0.75732	4.8056	0	0	0	0	1	0	1	-360	360;
	9003	9044	0.07378	0.06352	0	0	0	0	0	0	1	-360	360;
	9044	9004	0.03832	0.02894	0	0	0	0	0	0	1	-360	360;
	9004	9041	0.36614	2.456	0	0	0	0	1	0	1	-360	360;
	9004	9042	1.0593	5.4536	0	0	0	0	1	0	1	-360	360;
	9004	9043	0.1567	1.6994	0	0	0	0	1	0	1	-360	360;
	9003	9034	0.13006	1.3912	0	0	0	0	1	0	1	-360	360;
	9003	9035	0.54484	3.4572	0	0	0	0	1	0	1	-360	360;
	9003	9036	0.15426	1.6729	0	0	0	0	1	0	1	-360	360;
	9003	9037	0.3849	2.5712	0	0	0	0	1	0	1	-360	360;
	9003	9038	0.4412	2.9668	0	0	0	0	1	0	1	-360	360;
	9012	9121	0.23552	0.99036	0	0	0	0	0	0	1	-360	360;
	9053	9533	0	0.75	0	0	0	0	0.9583	0	1	-360	360;
	1	5	0.001	0.006	0	0	0	0	0	0	1	-360	360;
	2	6	0.001	0.009	0	0	0	0	0	0	1	-360	360;
	2	8	0.006	0.027	0.054	0	0	0	0	0	1	-360	360;
	3	7	0	0.003	0	0	0	0	0	0	1	-360	360;
	3	19	0.008	0.069	0.139	0	0	0	0	0	1	-360	360;
	3	150	0.001	0.007	0	0	0	0	0	0	1	-360	360;
	4	16	0.002	0.019	1.127	0	0	0	0	0	1	-360	360;
	5	9	0.006	0.029	0.018	0	0	0	0	0	1	-360	360;
	7	12	0.001	0.009	0.07	0	0	0	0	0	1	-360	360;
	7	131	0.001	0.007	0.014	0	0	0	0	0	1	-360	360;
	8	11	0.013	0.0595	0.033	0	0	0	0	0	1	-360	360;
	8	14	0.013	0.042	0.081	0	0	0	0	0	1	-360	360;
	9	11	0.006	0.027	0.013	0	0	0	0	0	1	-360	360;
	11	13	0.008	0.034	0.018	0	0	0	0	0	1	-360	360;
	12	21	0.002	0.015	0.118	0	0	0	0	0	1	-360	360;
	13	20	0.006	0.034	0.016	0	0	0	0	0	1	-360	360;
	14	15	0.014	0.042	0.097	0	0	0	0	0	1	-360	360;
	15	37	0.065	0.248	0.121	0	0	0	0	0	1	-360	360;
	15	89	0.099	0.248	0.035	0	0	0	0	0	1	-360	360;
	15	90	0.096	0.363	0.048	0	0	0	0	0	1	-360	360;
	16	42	0.002	0.022	1.28	0	0	0	0	0	1	-360	360;
	19	21	0.002	0.018	0.036	0	0	0	0	0	1	-360	360;
	19	87	0.013	0.08	0.151	0	0	0	0	0	1	-360	360;
	20	22	0.016	0.033	0.015	0	0	0	0	0	1	-360	360;
	20	27	0.069	0.186	0.098	0	0	0	0	0	1	-360	360;
	21	24	0.004	0.034	0.28	0	0	0	0	0	1	-360	360;
	22	23	0.052	0.111	0.05	0	0	0	0	0	1	-360	360;
	23	25	0.019	0.039	0.018	0	0	0	0	0	1	-360	360;
	24	319	0.007	0.068	0.134	0	0	0	0	0	1	-360	360;
	25	26	0.036	0.071	0.034	0	0	0	0	0	1	-360	360;
	26	27	0.045	0.12	0.065	0	0	0	0	0	1	-360	360;
	26	320	0.043	0.13	0.014	0	0	0	0	0	1	-360	360;
	33	34	0	0.063	0	0	0	0	0	0	1	-360	360;
	33	38	0.0025	0.012	0.013	0	0	0	0	0	1	-360	360;
	33	40	0.006	0.029	0.02	0	0	0	0	0	1	-360	360;
	33	41	0.007	0.043	0.026	0	0	0	0	0	1	-360	360;
	34	42	0.001	0.008	0.042	0	0	0	0	0	1	-360	360;
	35	72	0.012	0.06	0.008	0	0	0	0	0	1	-360	360;
	35	76	0.006	0.014	0.002	0	0	0	0	0	1	-360	360;
	35	77	0.01	0.029	0.003	0	0	0	0	0	1	-360	360;
	36	88	0.004	0.027	0.043	0	0	0	0	0	1	-360	360;
	37	38	0.008	0.047	0.008	0	0	0	0	0	1	-360	360;
	37	40	0.022	0.064	0.007	0	0	0	0	0	1	-360	360;
	37	41	0.01	0.036	0.02	0	0	0	0	0	1	-360	360;
	37	49	0.017	0.081	0.048	0	0	0	0	0	1	-360	360;
	37	89	0.102	0.254	0.033	0	0	0	0	0	1	-360	360;
	37	90	0.047	0.127	0.016	0	0	0	0	0	1	-360	360;
	38	41	0.008	0.037	0.02	0	0	0	0	0	1	-360	360;
	38	43	0.032	0.087	0.04	0	0	0	0	0	1	-360	360;
	39	42	0.0006	0.0064	0.404	0	0	0	0	0	1	-360	360;
	40	48	0.026	0.154	0.022	0	0	0	0	0	1	-360	360;
	41	42	0	0.029	0	0	0	0	0	0	1	-360	360;
	41	49	0.065	0.191	0.02	0	0	0	0	0	1	-360	360;
	41	51	0.031	0.089	0.036	0	0	0	0	0	1	-360	360;
	42	46	0.002	0.014	0.806	0	0	0	0	0	1	-360	360;
	43	44	0.026	0.072	0.035	0	0	0	0	0	1	-360	360;
	43	48	0.095	0.262	0.032	0	0	0	0	0	1	-360	360;
	43	53	0.013	0.039	0.016	0	0	0	0	0	1	-360	360;
	44	47	0.027	0.084	0.039	0	0	0	0	0	1	-360	360;
	44	54	0.028	0.084	0.037	0	0	0	0	0	1	-360	360;
	45	60	0.007	0.041	0.312	0	0	0	0	0	1	-360	360;
	45	74	0.009	0.054	0.411	0	0	0	0	0	1	-360	360;
	46	81	0.005	0.042	0.69	0	0	0	0	0	1	-360	360;
	47	73	0.052	0.145	0.073	0	0	0	0	0	1	-360	360;
	47	113	0.043	0.118	0.013	0	0	0	0	0	1	-360	360;
	48	107	0.025	0.062	0.007	0	0	0	0	0	1	-360	360;
	49	51	0.031	0.094	0.043	0	0	0	0	0	1	-360	360;
	51	52	0.037	0.109	0.049	0	0	0	0	0	1	-360	360;
	52	55	0.027	0.08	0.036	0	0	0	0	0	1	-360	360;
	53	54	0.025	0.073	0.035	0	0	0	0	0	1	-360	360;
	54	55	0.035	0.103	0.047	0	0	0	0	0	1	-360	360;
	55	57	0.065	0.169	0.082	0	0	0	0	0	1	-360	360;
	57	58	0.046	0.08	0.036	0	0	0	0	0	1	-360	360;
	57	63	0.159	0.537	0.071	0	0	0	0	0	1	-360	360;
	58	59	0.009	0.026	0.005	0	0	0	0	0	1	-360	360;
	59	61	0.002	0.013	0.015	0	0	0	0	0	1	-360	360;
	60	62	0.009	0.065	0.485	0	0	0	0	0	1	-360	360;
	62	64	0.016	0.105	0.203	0	0	0	0	0	1	-360	360;
	62	144	0.001	0.007	0.013	0	0	0	0	0	1	-360	360;
	63	526	0.0265	0.172	0.026	0	0	0	0	0	1	-360	360;
	69	211	0.051	0.232	0.028	0	0	0	0	0	1	-360	360;
	69	79	0.051	0.157	0.023	0	0	0	0	0	1	-360	360;
	70	71	0.032	0.1	0.062	0	0	0	0	0	1	-360	360;
	70	528	0.02	0.1234	0.028	0	0	0	0	0	1	-360	360;
	71	72	0.036	0.131	0.068	0	0	0	0	0	1	-360	360;
	71	73	0.034	0.099	0.047	0	0	0	0	0	1	-360	360;
	72	77	0.018	0.087	0.011	0	0	0	0	0	1	-360	360;
	72	531	0.0256	0.193	0	0	0	0	0	0	1	-360	360;
	73	76	0.021	0.057	0.03	0	0	0	0	0	1	-360	360;
	73	79	0.018	0.052	0.018	0	0	0	0	0	1	-360	360;
	74	88	0.004	0.027	0.05	0	0	0	0	0	1	-360	360;
	74	562	0.0286	0.2013	0.379	0	0	0	0	0	1	-360	360;
	76	77	0.016	0.043	0.004	0	0	0	0	0	1	-360	360;
	77	78	0.001	0.006	0.007	0	0	0	0	0	1	-360	360;
	77	80	0.014	0.07	0.038	0	0	0	0	0	1	-360	360;
	77	552	0.0891	0.2676	0.029	0	0	0	0	0	1	-360	360;
	77	609	0.0782	0.2127	0.022	0	0	0	0	0	1	-360	360;
	78	79	0.006	0.022	0.011	0	0	0	0	0	1	-360	360;
	78	84	0	0.036	0	0	0	0	0	0	1	-360	360;
	79	211	0.099	0.375	0.051	0	0	0	0	0	1	-360	360;
	80	211	0.022	0.107	0.058	0	0	0	0	0	1	-360	360;
	81	194	0.0035	0.033	0.53	0	0	0	0	0	1	-360	360;
	81	195	0.0035	0.033	0.53	0	0	0	0	0	1	-360	360;
	85	86	0.008	0.064	0.128	0	0	0	0	0	1	-360	360;
	86	87	0.012	0.093	0.183	0	0	0	0	0	1	-360	360;
	86	323	0.006	0.048	0.092	0	0	0	0	0	1	-360	360;
	89	91	0.047	0.119	0.014	0	0	0	0	0	1	-360	360;
	90	92	0.032	0.174	0.024	0	0	0	0	0	1	-360	360;
	91	94	0.1	0.253	0.031	0	0	0	0	0	1	-360	360;
	91	97	0.022	0.077	0.039	0	0	0	0	0	1	-360	360;
	92	103	0.019	0.144	0.017	0	0	0	0	0	1	-360	360;
	92	105	0.017	0.092	0.012	0	0	0	0	0	1	-360	360;
	94	97	0.278	0.427	0.043	0	0	0	0	0	1	-360	360;
	97	100	0.022	0.053	0.007	0	0	0	0	0	1	-360	360;
	97	102	0.038	0.092	0.012	0	0	0	0	0	1	-360	360;
	97	103	0.048	0.122	0.015	0	0	0	0	0	1	-360	360;
	98	100	0.024	0.064	0.007	0	0	0	0	0	1	-360	360;
	98	102	0.034	0.121	0.015	0	0	0	0	0	1	-360	360;
	99	107	0.053	0.135	0.017	0	0	0	0	0	1	-360	360;
	99	108	0.002	0.004	0.002	0	0	0	0	0	1	-360	360;
	99	109	0.045	0.354	0.044	0	0	0	0	0	1	-360	360;
	99	110	0.05	0.174	0.022	0	0	0	0	0	1	-360	360;
	100	102	0.016	0.038	0.004	0	0	0	0	0	1	-360	360;
	102	104	0.043	0.064	0.027	0	0	0	0	0	1	-360	360;
	103	105	0.019	0.062	0.008	0	0	0	0	0	1	-360	360;
	104	108	0.076	0.13	0.044	0	0	0	0	0	1	-360	360;
	104	322	0.044	0.124	0.015	0	0	0	0	0	1	-360	360;
	105	107	0.012	0.088	0.011	0	0	0	0	0	1	-360	360;
	105	110	0.157	0.4	0.047	0	0	0	0	0	1	-360	360;
	108	324	0.074	0.208	0.026	0	0	0	0	0	1	-360	360;
	109	110	0.07	0.184	0.021	0	0	0	0	0	1	-360	360;
	109	113	0.1	0.274	0.031	0	0	0	0	0	1	-360	360;
	109	114	0.109	0.393	0.036	0	0	0	0	0	1	-360	360;
	110	112	0.142	0.404	0.05	0	0	0	0	0	1	-360	360;
	112	114	0.017	0.042	0.006	0	0	0	0	0	1	-360	360;
	115	122	0.0036	0.0199	0.004	0	0	0	0	0	1	-360	360;
	116	120	0.002	0.1049	0.001	0	0	0	0	0	1	-360	360;
	117	118	0.0001	0.0018	0.017	0	0	0	0	0	1	-360	360;
	118	119	0	0.0271	0	0	0	0	0	0	1	-360	360;
	118	1201	0	0.6163	0	0	0	0	0	0	1	-360	360;
	1201	120	0	-0.3697	0	0	0	0	0	0	1	-360	360;
	118	121	0.0022	0.2915	0	0	0	0	0	0	1	-360	360;
	119	120	0	0.0339	0	0	0	0	0	0	1	-360	360;
	119	121	0	0.0582	0	0	0	0	0	0	1	-360	360;
	122	123	0.0808	0.2344	0.029	0	0	0	0	0	1	-360	360;
	122	125	0.0965	0.3669	0.054	0	0	0	0	0	1	-360	360;
	123	124	0.036	0.1076	0.117	0	0	0	0	0	1	-360	360;
	123	125	0.0476	0.1414	0.149	0	0	0	0	0	1	-360	360;
	125	126	0.0006	0.0197	0	0	0	0	0	0	1	-360	360;
	126	127	0.0059	0.0405	0.25	0	0	0	0	0	1	-360	360;
	126	129	0.0115	0.1106	0.185	0	0	0	0	0	1	-360	360;
	126	132	0.0198	0.1688	0.321	0	0	0	0	0	1	-360	360;
	126	157	0.005	0.05	0.33	0	0	0	0	0	1	-360	360;
	126	158	0.0077	0.0538	0.335	0	0	0	0	0	1	-360	360;
	126	169	0.0165	0.1157	0.171	0	0	0	0	0	1	-360	360;
	127	128	0.0059	0.0577	0.095	0	0	0	0	0	1	-360	360;
	127	134	0.0049	0.0336	0.208	0	0	0	0	0	1	-360	360;
	127	168	0.0059	0.0577	0.095	0	0	0	0	0	1	-360	360;
	128	130	0.0078	0.0773	0.126	0	0	0	0	0	1	-360	360;
	128	133	0.0026	0.0193	0.03	0	0	0	0	0	1	-360	360;
	129	130	0.0076	0.0752	0.122	0	0	0	0	0	1	-360	360;
	129	133	0.0021	0.0186	0.03	0	0	0	0	0	1	-360	360;
	130	132	0.0016	0.0164	0.026	0	0	0	0	0	1	-360	360;
	130	151	0.0017	0.0165	0.026	0	0	0	0	0	1	-360	360;
	130	167	0.0079	0.0793	0.127	0	0	0	0	0	1	-360	360;
	130	168	0.0078	0.0784	0.125	0	0	0	0	0	1	-360	360;
	133	137	0.0017	0.0117	0.289	0	0	0	0	0	1	-360	360;
	133	168	0.0026	0.0193	0.03	0	0	0	0	0	1	-360	360;
	133	169	0.0021	0.0186	0.03	0	0	0	0	0	1	-360	360;
	133	171	0.0002	0.0101	0	0	0	0	0	0	1	-360	360;
	134	135	0.0043	0.0293	0.18	0	0	0	0	0	1	-360	360;
	134	184	0.0039	0.0381	0.258	0	0	0	0	0	1	-360	360;
	135	136	0.0091	0.0623	0.385	0	0	0	0	0	1	-360	360;
	136	137	0.0125	0.089	0.54	0	0	0	0	0	1	-360	360;
	136	152	0.0056	0.039	0.953	0	0	0	0	0	1	-360	360;
	137	140	0.0015	0.0114	0.284	0	0	0	0	0	1	-360	360;
	137	181	0.0005	0.0034	0.021	0	0	0	0	0	1	-360	360;
	137	186	0.0007	0.0151	0.126	0	0	0	0	0	1	-360	360;
	137	188	0.0005	0.0034	0.021	0	0	0	0	0	1	-360	360;
	139	172	0.0562	0.2248	0.081	0	0	0	0	0	1	-360	360;
	140	141	0.012	0.0836	0.123	0	0	0	0	0	1	-360	360;
	140	142	0.0152	0.1132	0.684	0	0	0	0	0	1	-360	360;
	140	145	0.0468	0.3369	0.519	0	0	0	0	0	1	-360	360;
	140	146	0.043	0.3031	0.463	0	0	0	0	0	1	-360	360;
	140	147	0.0489	0.3492	0.538	0	0	0	0	0	1	-360	360;
	140	182	0.0013	0.0089	0.119	0	0	0	0	0	1	-360	360;
	141	146	0.0291	0.2267	0.342	0	0	0	0	0	1	-360	360;
	142	143	0.006	0.057	0.767	0	0	0	0	0	1	-360	360;
	143	145	0.0075	0.0773	0.119	0	0	0	0	0	1	-360	360;
	143	149	0.0127	0.0909	0.135	0	0	0	0	0	1	-360	360;
	145	146	0.0085	0.0588	0.087	0	0	0	0	0	1	-360	360;
	145	149	0.0218	0.1511	0.223	0	0	0	0	0	1	-360	360;
	146	147	0.0073	0.0504	0.074	0	0	0	0	0	1	-360	360;
	148	178	0.0523	0.1526	0.074	0	0	0	0	0	1	-360	360;
	148	179	0.1371	0.3919	0.076	0	0	0	0	0	1	-360	360;
	152	153	0.0137	0.0957	0.141	0	0	0	0	0	1	-360	360;
	153	161	0.0055	0.0288	0.19	0	0	0	0	0	1	-360	360;
	154	156	0.1746	0.3161	0.04	0	0	0	0	0	1	-360	360;
	154	183	0.0804	0.3054	0.045	0	0	0	0	0	1	-360	360;
	155	161	0.011	0.0568	0.388	0	0	0	0	0	1	-360	360;
	157	159	0.0008	0.0098	0.069	0	0	0	0	0	1	-360	360;
	158	159	0.0029	0.0285	0.19	0	0	0	0	0	1	-360	360;
	158	160	0.0066	0.0448	0.277	0	0	0	0	0	1	-360	360;
	162	164	0.0024	0.0326	0.236	0	0	0	0	0	1	-360	360;
	162	165	0.0018	0.0245	1.662	0	0	0	0	0	1	-360	360;
	163	164	0.0044	0.0514	3.597	0	0	0	0	0	1	-360	360;
	165	166	0.0002	0.0123	0	0	0	0	0	0	1	-360	360;
	167	169	0.0018	0.0178	0.029	0	0	0	0	0	1	-360	360;
	172	173	0.0669	0.4843	0.063	0	0	0	0	0	1	-360	360;
	172	174	0.0558	0.221	0.031	0	0	0	0	0	1	-360	360;
	173	174	0.0807	0.3331	0.049	0	0	0	0	0	1	-360	360;
	173	175	0.0739	0.3071	0.043	0	0	0	0	0	1	-360	360;
	173	176	0.1799	0.5017	0.069	0	0	0	0	0	1	-360	360;
	175	176	0.0904	0.3626	0.048	0	0	0	0	0	1	-360	360;
	175	179	0.077	0.3092	0.054	0	0	0	0	0	1	-360	360;
	176	177	0.0251	0.0829	0.047	0	0	0	0	0	1	-360	360;
	177	178	0.0222	0.0847	0.05	0	0	0	0	0	1	-360	360;
	178	179	0.0498	0.1855	0.029	0	0	0	0	0	1	-360	360;
	178	180	0.0061	0.029	0.084	0	0	0	0	0	1	-360	360;
	181	138	0.0004	0.0202	0	0	0	0	0	0	1	-360	360;
	181	187	0.0004	0.0083	0.115	0	0	0	0	0	1	-360	360;
	184	185	0.0025	0.0245	0.164	0	0	0	0	0	1	-360	360;
	186	188	0.0007	0.0086	0.115	0	0	0	0	0	1	-360	360;
	187	188	0.0007	0.0086	0.115	0	0	0	0	0	1	-360	360;
	188	138	0.0004	0.0202	0	0	0	0	0	0	1	-360	360;
	189	208	0.033	0.095	0	0	0	0	0	0	1	-360	360;
	189	209	0.046	0.069	0	0	0	0	0	0	1	-360	360;
	190	231	0.0004	0.0022	6.2	0	0	0	0	0	1	-360	360;
	190	240	0	0.0275	0	0	0	0	0	0	1	-360	360;
	191	192	0.003	0.048	0	0	0	0	0	0	1	-360	360;
	192	225	0.002	0.009	0	0	0	0	0	0	1	-360	360;
	193	205	0.045	0.063	0	0	0	0	0	0	1	-360	360;
	193	208	0.048	0.127	0	0	0	0	0	0	1	-360	360;
	194	219	0.0031	0.0286	0.5	0	0	0	0	0	1	-360	360;
	194	664	0.0024	0.0355	0.36	0	0	0	0	0	1	-360	360;
	195	219	0.0031	0.0286	0.5	0	0	0	0	0	1	-360	360;
	196	197	0.014	0.04	0.004	0	0	0	0	0	1	-360	360;
	196	210	0.03	0.081	0.01	0	0	0	0	0	1	-360	360;
	197	198	0.01	0.06	0.009	0	0	0	0	0	1	-360	360;
	197	211	0.015	0.04	0.006	0	0	0	0	0	1	-360	360;
	198	202	0.332	0.688	0	0	0	0	0	0	1	-360	360;
	198	203	0.009	0.046	0.025	0	0	0	0	0	1	-360	360;
	198	210	0.02	0.073	0.008	0	0	0	0	0	1	-360	360;
	198	211	0.034	0.109	0.032	0	0	0	0	0	1	-360	360;
	199	200	0.076	0.135	0.009	0	0	0	0	0	1	-360	360;
	199	210	0.04	0.102	0.005	0	0	0	0	0	1	-360	360;
	200	210	0.081	0.128	0.014	0	0	0	0	0	1	-360	360;
	201	204	0.124	0.183	0	0	0	0	0	0	1	-360	360;
	203	211	0.01	0.059	0.008	0	0	0	0	0	1	-360	360;
	204	205	0.046	0.068	0	0	0	0	0	0	1	-360	360;
	205	206	0.302	0.446	0	0	0	0	0	0	1	-360	360;
	206	207	0.073	0.093	0	0	0	0	0	0	1	-360	360;
	206	208	0.24	0.421	0	0	0	0	0	0	1	-360	360;
	212	215	0.0139	0.0778	0.086	0	0	0	0	0	1	-360	360;
	213	214	0.0025	0.038	0	0	0	0	1	0	1	-360	360;
	214	215	0.0017	0.0185	0.02	0	0	0	0	0	1	-360	360;
	214	242	0.0015	0.0108	0.002	0	0	0	0	0	1	-360	360;
	215	216	0.0045	0.0249	0.026	0	0	0	0	0	1	-360	360;
	216	217	0.004	0.0497	0.018	0	0	0	0	0	1	-360	360;
	217	218	0	0.0456	0	0	0	0	0	0	1	-360	360;
	217	219	0.0005	0.0177	0.02	0	0	0	0	0	1	-360	360;
	217	220	0.0027	0.0395	0.832	0	0	0	0	0	1	-360	360;
	219	237	0.0003	0.0018	5.2	0	0	0	0	0	1	-360	360;
	220	218	0.0037	0.0484	0.43	0	0	0	0	0	1	-360	360;
	220	221	0.001	0.0295	0.503	0	0	0	0	0	1	-360	360;
	220	238	0.0016	0.0046	0.402	0	0	0	0	0	1	-360	360;
	221	223	0.0003	0.0013	1	0	0	0	0	0	1	-360	360;
	222	237	0.0014	0.0514	0.33	0	0	0	1	0	1	-360	360;
	224	225	0.01	0.064	0.48	0	0	0	0	0	1	-360	360;
	224	226	0.0019	0.0081	0.86	0	0	0	0	0	1	-360	360;
	225	191	0.001	0.061	0	0	0	0	0	0	1	-360	360;
	226	231	0.0005	0.0212	0	0	0	0	0	0	1	-360	360;
	227	231	0.0009	0.0472	0.186	0	0	0	1	0	1	-360	360;
	228	229	0.0019	0.0087	1.28	0	0	0	0	0	1	-360	360;
	228	231	0.0026	0.0917	0	0	0	0	0	0	1	-360	360;
	228	234	0.0013	0.0288	0.81	0	0	0	0	0	1	-360	360;
	229	190	0	0.0626	0	0	0	0	0	0	1	-360	360;
	231	232	0.0002	0.0069	1.364	0	0	0	0	0	1	-360	360;
	231	237	0.0001	0.0006	3.57	0	0	0	0	0	1	-360	360;
	232	233	0.0017	0.0485	0	0	0	0	0	0	1	-360	360;
	234	235	0.0002	0.0259	0.144	0	0	0	0	0	1	-360	360;
	234	237	0.0006	0.0272	0	0	0	0	0	0	1	-360	360;
	235	238	0.0002	0.0006	0.8	0	0	0	0	0	1	-360	360;
	241	237	0.0005	0.0154	0	0	0	0	1	0	1	-360	360;
	240	281	0.0003	0.0043	0.009	0	0	0	0	0	1	-360	360;
	242	245	0.0082	0.0851	0	0	0	0	0	0	1	-360	360;
	242	247	0.0112	0.0723	0	0	0	0	0	0	1	-360	360;
	243	244	0.0127	0.0355	0	0	0	0	0	0	1	-360	360;
	243	245	0.0326	0.1804	0	0	0	0	0	0	1	-360	360;
	244	246	0.0195	0.0551	0	0	0	0	0	0	1	-360	360;
	245	246	0.0157	0.0732	0	0	0	0	0	0	1	-360	360;
	245	247	0.036	0.2119	0	0	0	0	0	0	1	-360	360;
	246	247	0.0268	0.1285	0	0	0	0	0	0	1	-360	360;
	247	248	0.0428	0.1215	0	0	0	0	0	0	1	-360	360;
	248	249	0.0351	0.1004	0	0	0	0	0	0	1	-360	360;
	249	250	0.0616	0.1857	0	0	0	0	0	0	1	-360	360;
	3	1	0	0.052	0	0	0	0	0.947	0	1	-360	360;
	3	2	0	0.052	0	0	0	0	0.956	0	1	-360	360;
	3	4	0	0.005	0	0	0	0	0.971	0	1	-360	360;
	7	5	0	0.039	0	0	0	0	0.948	0	1	-360	360;
	7	6	0	0.039	0	0	0	0	0.959	0	1	-360	360;
	10	11	0	0.089	0	0	0	0	1.046	0	1	-360	360;
	12	10	0	0.053	0	0	0	0	0.985	0	1	-360	360;
	15	17	0.0194	0.0311	0	0	0	0	0.9561	0	1	-360	360;
	16	15	0.001	0.038	0	0	0	0	0.971	0	1	-360	360;
	21	20	0	0.014	0	0	0	0	0.952	0	1	-360	360;
	24	23	0	0.064	0	0	0	0	0.943	0	1	-360	360;
	36	35	0	0.047	0	0	0	0	1.01	0	1	-360	360;
	45	44	0	0.02	0	0	0	0	1.008	0	1	-360	360;
	45	46	0	0.021	0	0	0	0	1	0	1	-360	360;
	62	61	0	0.059	0	0	0	0	0.975	0	1	-360	360;
	63	64	0	0.038	0	0	0	0	1.017	0	1	-360	360;
	73	74	0	0.0244	0	0	0	0	1	0	1	-360	360;
	81	88	0	0.02	0	0	0	0	1	0	1	-360	360;
	85	99	0	0.048	0	0	0	0	1	0	1	-360	360;
	86	102	0	0.048	0	0	0	0	1	0	1	-360	360;
	87	94	0	0.046	0	0	0	0	1.015	0	1	-360	360;
	114	207	0	0.149	0	0	0	0	0.967	0	1	-360	360;
	116	124	0.0052	0.0174	0	0	0	0	1.01	0	1	-360	360;
	121	115	0	0.028	0	0	0	0	1.05	0	1	-360	360;
	122	157	0.0005	0.0195	0	0	0	0	1	0	1	-360	360;
	130	131	0	0.018	0	0	0	0	1.0522	0	1	-360	360;
	130	150	0	0.014	0	0	0	0	1.0522	0	1	-360	360;
	132	170	0.001	0.0402	0	0	0	0	1.05	0	1	-360	360;
	141	174	0.0024	0.0603	0	0	0	0	0.975	0	1	-360	360;
	142	175	0.0024	0.0498	-0.087	0	0	0	1	0	1	-360	360;
	143	144	0	0.0833	0	0	0	0	1.035	0	1	-360	360;
	143	148	0.0013	0.0371	0	0	0	0	0.9565	0	1	-360	360;
	145	180	0.0005	0.0182	0	0	0	0	1	0	1	-360	360;
	151	170	0.001	0.0392	0	0	0	0	1.05	0	1	-360	360;
	153	183	0.0027	0.0639	0	0	0	0	1.073	0	1	-360	360;
	155	156	0.0008	0.0256	0	0	0	0	1.05	0	1	-360	360;
	159	117	0	0.016	0	0	0	0	1.0506	0	1	-360	360;
	160	124	0.0012	0.0396	0	0	0	0	0.975	0	1	-360	360;
	163	137	0.0013	0.0384	-0.057	0	0	0	0.98	0	1	-360	360;
	164	155	0.0009	0.0231	-0.033	0	0	0	0.956	0	1	-360	360;
	182	139	0.0003	0.0131	0	0	0	0	1.05	0	1	-360	360;
	189	210	0	0.252	0	0	0	0	1.03	0	1	-360	360;
	193	196	0	0.237	0	0	0	0	1.03	0	1	-360	360;
	195	212	0.0008	0.0366	0	0	0	0	0.985	0	1	-360	360;
	200	248	0	0.22	0	0	0	0	1	0	1	-360	360;
	201	69	0	0.098	0	0	0	0	1.03	0	1	-360	360;
	202	211	0	0.128	0	0	0	0	1.01	0	1	-360	360;
	204	2040	0.02	0.204	-0.012	0	0	0	1.05	0	1	-360	360;
	209	198	0.026	0.211	0	0	0	0	1.03	0	1	-360	360;
	211	212	0.003	0.0122	0	0	0	0	1	0	1	-360	360;
	218	219	0.001	0.0354	-0.01	0	0	0	0.97	0	1	-360	360;
	223	224	0.0012	0.0195	-0.364	0	0	0	1	0	1	-360	360;
	229	230	0.001	0.0332	0	0	0	0	1.02	0	1	-360	360;
	234	236	0.0005	0.016	0	0	0	0	1.07	0	1	-360	360;
	238	239	0.0005	0.016	0	0	0	0	1.02	0	1	-360	360;
	196	2040	0.0001	0.02	0	0	0	0	1	0	1	-360	360;
	119	1190	0.001	0.023	0	0	0	0	1.0223	0	1	-360	360;
	120	1200	0	0.023	0	0	0	0	0.9284	0	1	-360	360;
	7002	2	0.001	0.0146	0	0	0	0	1	0	1	-360	360;
	7003	3	0	0.01054	0	0	0	0	1	0	1	-360	360;
	7061	61	0	0.0238	0	0	0	0	1	0	1	-360	360;
	7062	62	0	0.03214	0	0	0	0	0.95	0	1	-360	360;
	7166	166	0	0.0154	0	0	0	0	1	0	1	-360	360;
	7024	24	0	0.0289	0	0	0	0	1	0	1	-360	360;
	7001	1	0	0.01953	0	0	0	0	1	0	1	-360	360;
	7130	130	0	0.0193	0	0	0	0	1	0	1	-360	360;
	7011	11	0	0.01923	0	0	0	0	1	0	1	-360	360;
	7023	23	0	0.023	0	0	0	0	1	0	1	-360	360;
	7049	49	0	0.0124	0	0	0	0	1	0	1	-360	360;
	7139	139	0	0.0167	0	0	0	0	1	0	1	-360	360;
	7012	12	0	0.0312	0	0	0	0	1	0	1	-360	360;
	7017	17	0	0.01654	0	0	0	0	0.942	0	1	-360	360;
	7039	39	0	0.03159	0	0	0	0	0.965	0	1	-360	360;
	7057	57	0	0.05347	0	0	0	0	0.95	0	1	-360	360;
	7044	44	0	0.18181	0	0	0	0	0.942	0	1	-360	360;
	7055	55	0	0.19607	0	0	0	0	0.942	0	1	-360	360;
	7071	71	0	0.06896	0	0	0	0	0.9565	0	1	-360	360;
];

%%-----  OPF Data  -----%%
%% generator cost data
%	1	startup	shutdown	n	x1	y1	...	xn	yn
%	2	startup	shutdown	n	c(n-1)	...	c0
mpc.gencost = [
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.0266666667	20	0;
	2	0	0	3	0.064516129	20	0;
	2	0	0	3	0.0344827586	20	0;
	2	0	0	3	0.147058824	20	0;
	2	0	0	3	0.0854700855	20	0;
	2	0	0	3	0.00518134715	20	0;
	2	0	0	3	0.0416666667	20	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.0355871886	20	0;
	2	0	0	3	0.0143678161	20	0;
	2	0	0	3	0.119047619	20	0;
	2	0	0	3	0.0460829493	20	0;
	2	0	0	3	0.0970873786	20	0;
	2	0	0	3	0.0268817204	20	0;
	2	0	0	3	0.0462962963	20	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.0487804878	20	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.0438596491	20	0;
	2	0	0	3	0.119047619	20	0;
	2	0	0	3	0.05	20	0;
	2	0	0	3	0.00833333333	20	0;
	2	0	0	3	0.00833333333	20	0;
	2	0	0	3	0.0210526316	20	0;
	2	0	0	3	0.00506842372	20	0;
	2	0	0	3	0.0235849057	20	0;
	2	0	0	3	0.0367647059	20	0;
	2	0	0	3	0.1	20	0;
	2	0	0	3	0.0222222222	20	0;
	2	0	0	3	0.04	20	0;
	2	0	0	3	0.0330033003	20	0;
	2	0	0	3	0.0289855072	20	0;
	2	0	0	3	0.0333333333	20	0;
	2	0	0	3	0.0166666667	20	0;
	2	0	0	3	0.04	20	0;
	2	0	0	3	0.0181818182	20	0;
	2	0	0	3	0.0173783084	20	0;
	2	0	0	3	0.0588235294	20	0;
	2	0	0	3	0.119047619	20	0;
	2	0	0	3	0.0214132762	20	0;
	2	0	0	3	0.0160513644	20	0;
	2	0	0	3	0.00826446281	20	0;
	2	0	0	3	0.0427350427	20	0;
	2	0	0	3	0.0268817204	20	0;
	2	0	0	3	0.0303030303	20	0;
	2	0	0	3	0.0540540541	20	0;
	2	0	0	3	0.0243902439	20	0;
	2	0	0	3	0.02	20	0;
	2	0	0	3	0.27027027	20	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.222222222	20	0;
	2	0	0	3	0.0606060606	20	0;
	2	0	0	3	0.025	20	0;
	2	0	0	3	0.025	20	0;
	2	0	0	3	0.0862068966	20	0;
	2	0	0	3	0.00773993808	20	0;
	2	0	0	3	0.0142857143	20	0;
	2	0	0	3	0.0180831826	20	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.01	40	0;
	2	0	0	3	0.2	20	0;
	2	0	0	3	1.25	20	0;
];

%% bus names
mpc.bus_name = {
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'2';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'1';
	'3';
	'3';
	'3';
	'3';
	'3';
	'1';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'3';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'3';
	'2';
	'2';
	'2';
	'3';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'2';
	'2';
	'2';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
	'1';
};
