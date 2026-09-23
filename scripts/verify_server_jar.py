"""Check that the distributable has no client requirement or test entrypoint."""
import json
import pathlib
import sys
import zipfile

jar = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else next(pathlib.Path('build/libs').glob('*.jar'))
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    metadata = json.loads(archive.read('fabric.mod.json'))
    assert metadata['environment'] == 'server'
    assert metadata['entrypoints'] == {'main': ['dev.cudzer.cobblemonsizevariation.fabric.CobblemonSizeVariationFabric']}
    assert metadata['mixins'] == ['cobblemonsizevariation-breeding.mixins.json']
    mixins = json.loads(archive.read(metadata['mixins'][0]))
    assert 'client' not in mixins
    assert mixins['mixins'] == ['BreedingParentsMixin', 'BreedingHatchMixin']
    assert mixins['plugin'] == 'dev.cudzer.cobblemonsizevariation.compat.BreedingMixinPlugin'
    assert 'cobblemon' in metadata['depends']
    assert not any('/recipe/' in n or n.startswith('assets/') or '/smoke/' in n for n in names)
    for name in names:
        if name.endswith('.class'):
            data = archive.read(name)
            assert b'net/minecraft/client/' not in data, name
            assert b'dev/cudzer/cobblemonsizevariation/network/' not in data, name
print(f'PASS: server-only release contents: {jar.name}')
