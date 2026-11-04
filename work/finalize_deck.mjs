import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { finalizePresentation } from '/home/chax/.codex/plugins/cache/openai-primary-runtime/presentations/26.1026.10807/skills/presentations/container_tools/artifact_tool_utils.mjs';

const workspaceDir = '/home/chax/work/Pesalock/projects/Ndani/reliable-money-transfer';
const candidatePath = path.join(workspaceDir, 'work/deck-build/reliable-money-transfer-candidate.pptx');
const finalPath = path.join(workspaceDir, 'outputs/reliable-money-transfer-architecture.pptx');
const result = await finalizePresentation({
  workspaceDir,
  candidatePath,
  finalPath,
  pythonExecutable: '/usr/bin/python3',
  integrityValidatorPath: '/home/chax/.codex/plugins/cache/openai-primary-runtime/presentations/26.1026.10807/skills/presentations/container_tools/inspect_presentation_package_integrity.py',
  layoutValidatorPath: '/home/chax/.codex/plugins/cache/openai-primary-runtime/presentations/26.1026.10807/skills/presentations/container_tools/inspect_presentation_layout_geometry.py',
  layoutArgs: ['--expected-slide-size-emu', '12192000,6858000', '--validate-bullet-geometry', '--validate-heading-fit'],
  requiredNativeTableOwnerSlides: [],
  fontPolicy: { basis: 'design', families: ['Arial'] },
  verifyArtifactToolImport: true,
  receiptPath: path.join(workspaceDir, 'work/deck-build/reliable-money-transfer-architecture.validation.json'),
});
console.log(JSON.stringify(result));

