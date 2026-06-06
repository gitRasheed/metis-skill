param(
    [string]$Model = "gpt-5.5",
    [string]$Reasoning = "xhigh",
    [string]$CaseSet = "cases-v3",
    [string[]]$Cases = @(),
    [string[]]$Conditions = @("baseline", "metis-core", "metis-examples")
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$caseRoot = Join-Path $repoRoot "evals\$CaseSet"
$runDate = Get-Date -Format "yyyy-MM-dd"
$runRoot = Join-Path $repoRoot "eval-runs\$runDate\$CaseSet\$Model-$Reasoning"
$cleanCodexHome = Join-Path $runRoot ".codex-home"
$userCodexHome = Join-Path $env:USERPROFILE ".codex"
$userAuth = Join-Path $userCodexHome "auth.json"
$globalMetis = Join-Path $env:USERPROFILE ".agents\skills\metis"
$hiddenMetis = Join-Path ([System.IO.Path]::GetTempPath()) ("metis-hidden-for-eval-" + [guid]::NewGuid().ToString("N"))
$metisSource = Join-Path $repoRoot "skills\metis\SKILL.md"
$examplesSource = Join-Path $repoRoot "skills\metis\references\examples.md"

if (-not (Test-Path $userAuth)) {
    throw "Codex auth not found at $userAuth"
}

$metisSkill = Get-Content -Raw -LiteralPath $metisSource
$metisCoreSkill = $metisSkill -replace '(?m)^- For optional concrete do/don''t examples when the task is complex or ambiguous: `references/examples\.md`\r?\n?', ""

if ($Conditions -contains "metis-examples" -and -not (Test-Path $examplesSource)) {
    throw "Metis examples reference not found at $examplesSource"
}

New-Item -ItemType Directory -Force $cleanCodexHome | Out-Null
Copy-Item -LiteralPath $userAuth -Destination (Join-Path $cleanCodexHome "auth.json") -Force

if ($Cases.Count -eq 0) {
    $Cases = Get-ChildItem -LiteralPath $caseRoot -Directory | Select-Object -ExpandProperty Name
}

$renamedMetis = $false

try {
    if (Test-Path $globalMetis) {
        Move-Item -LiteralPath $globalMetis -Destination $hiddenMetis
        $renamedMetis = $true
    }

    foreach ($case in $Cases) {
        $promptSource = Join-Path $caseRoot "$case\prompt.md"
        if (-not (Test-Path $promptSource)) {
            throw "Missing prompt: $promptSource"
        }

        foreach ($condition in $Conditions) {
            $conditionRoot = Join-Path $runRoot "$case\$condition"
            $workspace = Join-Path $conditionRoot "workspace"
            New-Item -ItemType Directory -Force $workspace | Out-Null
            Copy-Item -LiteralPath $promptSource -Destination (Join-Path $workspace "prompt.md") -Force

            switch ($condition) {
                "baseline" {
                    $runnerPrompt = "Read only prompt.md from the current directory. Answer the task in prompt.md. Do not inspect any other files. Do not edit files."
                }
                "metis-core" {
                    Set-Content -LiteralPath (Join-Path $workspace "METIS_SKILL.md") -Value $metisCoreSkill -NoNewline
                    $runnerPrompt = "Read only prompt.md and METIS_SKILL.md from the current directory. Use METIS_SKILL.md as the coding style guide. Answer the task in prompt.md. Do not inspect any other files. Do not edit files."
                }
                "metis-examples" {
                    Set-Content -LiteralPath (Join-Path $workspace "METIS_SKILL.md") -Value $metisSkill -NoNewline
                    $referencesRoot = Join-Path $workspace "references"
                    New-Item -ItemType Directory -Force $referencesRoot | Out-Null
                    Copy-Item -LiteralPath $examplesSource -Destination (Join-Path $referencesRoot "examples.md") -Force
                    $runnerPrompt = "Read only prompt.md, METIS_SKILL.md, and references/examples.md from the current directory. Use METIS_SKILL.md and references/examples.md as the coding style guide. Answer the task in prompt.md. Do not inspect any other files. Do not edit files."
                }
                default {
                    throw "Unknown condition: $condition"
                }
            }

            $env:CODEX_HOME = $cleanCodexHome
            Push-Location $workspace
            try {
                $promptInputPath = Join-Path $conditionRoot "prompt-input.json"
                $eventsPath = Join-Path $conditionRoot "events.jsonl"
                $finalPath = Join-Path $conditionRoot "final.md"

                $previousErrorActionPreference = $ErrorActionPreference
                $ErrorActionPreference = "Continue"

                & codex debug prompt-input --disable plugins $runnerPrompt *> $promptInputPath
                if ($LASTEXITCODE -ne 0) {
                    throw "codex debug prompt-input failed for $case/$condition with exit code $LASTEXITCODE"
                }

                & codex exec `
                    --ephemeral `
                    --ignore-user-config `
                    --ignore-rules `
                    --disable plugins `
                    --json `
                    -m $Model `
                    -c "model_reasoning_effort=`"$Reasoning`"" `
                    -s read-only `
                    --skip-git-repo-check `
                    -o $finalPath `
                    $runnerPrompt *> $eventsPath
                if ($LASTEXITCODE -ne 0) {
                    throw "codex exec failed for $case/$condition with exit code $LASTEXITCODE"
                }

                $ErrorActionPreference = $previousErrorActionPreference
            } finally {
                if (Test-Path variable:previousErrorActionPreference) {
                    $ErrorActionPreference = $previousErrorActionPreference
                }
                Pop-Location
            }
        }
    }
} finally {
    if ($renamedMetis -and (Test-Path $hiddenMetis)) {
        Move-Item -LiteralPath $hiddenMetis -Destination $globalMetis
    }
}
